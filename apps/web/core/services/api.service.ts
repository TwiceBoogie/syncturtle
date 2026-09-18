import { CsrfTokenCache, revokedCurrentSession, shouldRetryInvalidCsrf } from "./csrf-token-cache";
import type { ICsrfTokenData } from "@syncturtle/types";

export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export interface IRequestConfig {
  params?: Record<string, string | number | boolean | null | undefined>;
  headers?: HeadersInit;
  body?: unknown;
  signal?: AbortSignal;
  credentials?: RequestCredentials;
  csrf?: boolean;
  validateStatus?: (status: number) => boolean;
  /**
   * Hard optout. use for public/bootstrap endpoints where 401 is expected but shouldn't really trigger refresh
   */
  skipAuthRefresh?: boolean;
  /**
   * Opt-in refresh. only endpoints that are truly protected should attempt silent refresh if this is set to true
   */
  authRefresh?: boolean;
  /**
   * Internal retry counter used to prevent infinite refresh loops
   */
  _retryAttempt?: number;
  _csrfRetryAttempt?: number;
}

export interface IHttpResponse<T> {
  data: T;
  status: number;
  headers: Headers;
  raw: Response;
}

export class HttpError<T> extends Error {
  status: number;
  data: T | null;
  headers: Headers;
  raw: Response;

  constructor(response: Response, data: T | null) {
    super(`HTTP Error ${response.status}`);
    this.status = response.status;
    this.data = data;
    this.headers = response.headers;
    this.raw = response;

    Object.setPrototypeOf(this, HttpError.prototype);
  }
}

export abstract class APIService {
  protected baseUrl: string;

  /**
   * All APIService subclasses share these promises in the same browser runtime
   */
  private static readonly csrfCache = new CsrfTokenCache();
  private static refreshPromise: Promise<void> | null = null;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  protected requestCsrfToken(): Promise<ICsrfTokenData> {
    return APIService.csrfCache.get(async () => {
      const response = await this.get<ICsrfTokenData>("/api/get-csrf-token", { csrf: false, skipAuthRefresh: true });
      return response.data;
    });
  }

  protected invalidateCsrfToken(): void {
    APIService.csrfCache.invalidate();
  }

  private async refreshSession(): Promise<void> {
    if (!APIService.refreshPromise) {
      APIService.refreshPromise = this.request<void>("POST", "/auth/refresh", {
        // keep refresh from recursively trying to refresh itself
        skipAuthRefresh: true,
        // CSRF remains enabled for the cookie-authenticated refresh request.
        // accept 401/403 as parsed responses so we can convert them into a normal "refresh failed" error
        validateStatus: (status) => status === 200 || status === 204 || status === 401 || status === 403,
      })
        .then((res) => {
          if (res.status !== 200 && res.status !== 204) {
            throw new Error("Refresh session failed");
          }
        })
        .finally(() => {
          APIService.refreshPromise = null;
        });
    }
    return APIService.refreshPromise;
  }

  private shouldAttachCsrf(method: HttpMethod, config: IRequestConfig): boolean {
    const unsafe = method === "POST" || method === "DELETE" || method === "PATCH" || method === "PUT";
    return config.csrf !== false && unsafe;
  }

  private async withCsrfIfNeeded(method: HttpMethod, config: IRequestConfig): Promise<IRequestConfig> {
    if (!this.shouldAttachCsrf(method, config)) {
      return config;
    }

    const token = await this.requestCsrfToken();
    const headers = new Headers(config.headers);
    headers.set("X-CSRF-Token", token.csrfToken);

    return { ...config, headers };
  }

  private buildUrl(path: string, params?: IRequestConfig["params"]): string {
    const url = new URL(path, this.baseUrl === "" ? window.location.origin : this.baseUrl);

    if (params) {
      Object.entries(params).forEach(([key, value]) => {
        if (value === undefined || value === null) {
          return;
        }
        url.searchParams.set(key, String(value));
      });
    }

    return url.toString();
  }

  private shouldAttemptRefresh(path: string, status: number, config: IRequestConfig): boolean {
    if (status !== 401) {
      return false;
    }
    /**
     * Refresh is now opt-in.
     *
     * This key behavior change public/bootstrap endpoints can return 401 without triggering /auth/refresh
     */
    if (config.authRefresh !== true) {
      return false;
    }
    // Hard opt-out still wins
    if (config.skipAuthRefresh === true) {
      return false;
    }
    // Prevent infinite loops
    if ((config._retryAttempt ?? 0) >= 1) {
      return false;
    }
    // never refresh the refresh endpoint
    if (path.startsWith("/auth/refresh")) {
      return false;
    }
    // never refresh the CSRF endpoint
    if (path.startsWith("/api/get-csrf-token")) {
      return false;
    }
    return true;
  }

  private async parseResponseBody(response: Response): Promise<unknown> {
    const text = await response.text();
    if (!text) {
      return null;
    }

    try {
      return JSON.parse(text);
    } catch {
      return text;
    }
  }

  private async request<T>(method: HttpMethod, path: string, config: IRequestConfig = {}): Promise<IHttpResponse<T>> {
    const configWithCsrf = await this.withCsrfIfNeeded(method, config);
    const { params, headers, body, signal, credentials, validateStatus } = configWithCsrf;
    const requestHeaders = new Headers(headers);
    const init: RequestInit = { method, headers: requestHeaders, credentials: credentials ?? "include" };

    if (signal) {
      init.signal = signal;
    }

    if (body !== undefined && body !== null && method !== "GET") {
      if (body instanceof FormData || body instanceof URLSearchParams || typeof body === "string") {
        init.body = body;
      } else {
        if (!requestHeaders.has("Content-Type")) {
          requestHeaders.set("Content-Type", "application/json");
        }
        init.body = JSON.stringify(body);
      }
    }

    const response = await fetch(this.buildUrl(path, params), init);
    const parsed = await this.parseResponseBody(response);

    // refresh before validate status so caller accepts some 4xx, it still silent refresh on 401
    if (this.shouldAttemptRefresh(path, response.status, configWithCsrf)) {
      try {
        await this.refreshSession();
        return this.request<T>(method, path, {
          ...config,
          _retryAttempt: (config._retryAttempt ?? 0) + 1,
        });
      } catch {
        throw new HttpError(response, parsed);
      }
    }

    if (
      this.shouldAttachCsrf(method, configWithCsrf) &&
      shouldRetryInvalidCsrf(response.status, parsed, config._csrfRetryAttempt ?? 0)
    ) {
      this.invalidateCsrfToken();
      return this.request<T>(method, path, { ...config, _csrfRetryAttempt: (config._csrfRetryAttempt ?? 0) + 1 });
    }

    const isOk = validateStatus ? validateStatus(response.status) : response.ok;

    if (!isOk) {
      throw new HttpError<T>(response, parsed as T | null);
    }

    if (revokedCurrentSession(parsed)) {
      this.invalidateCsrfToken();
    }

    return {
      data: parsed as T,
      status: response.status,
      headers: response.headers,
      raw: response,
    };
  }

  protected get<T>(url: string, config: Omit<IRequestConfig, "body"> = {}): Promise<IHttpResponse<T>> {
    return this.request<T>("GET", url, config);
  }

  protected patch<T>(
    url: string,
    body?: unknown,
    config: Omit<IRequestConfig, "body"> = {}
  ): Promise<IHttpResponse<T>> {
    return this.request<T>("PATCH", url, { ...config, body });
  }

  protected post<T>(url: string, body?: unknown, config: Omit<IRequestConfig, "body"> = {}): Promise<IHttpResponse<T>> {
    return this.request<T>("POST", url, { ...config, body });
  }

  protected delete<T>(
    url: string,
    body?: unknown,
    config: Omit<IRequestConfig, "body"> = {}
  ): Promise<IHttpResponse<T>> {
    return this.request<T>("DELETE", url, { ...config, body });
  }
}
