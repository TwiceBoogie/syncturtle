export type HttpMethod = "GET" | "POST" | "PUT" | "PATCH" | "DELETE";

export interface IRequestConfig {
  params?: Record<string, string | number | boolean | null | undefined>;
  headers?: HeadersInit;
  body?: unknown;
  signal?: AbortSignal;
  credentials?: RequestCredentials;
  validateStatus?: (status: number) => boolean;
  authRefresh?: boolean;
  skipAuthRefresh?: boolean;
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
  private static refreshPromise: Promise<void> | null = null;
  private static readonly BROWSER_HEADER = "X-ST-Browser-Request";
  private static readonly REJECTION_HEADER = "X-ST-Gateway-Auth-Rejection";

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  private buildUrl(path: string, params?: IRequestConfig["params"]): string {
    const url = new URL(path, this.baseUrl === "" ? window.location.origin : this.baseUrl);
    if (params) {
      for (const [key, value] of Object.entries(params)) {
        if (value !== undefined && value !== null) {
          url.searchParams.set(key, String(value));
        }
      }
    }
    return url.toString();
  }

  private async parseResponseBody(response: Response): Promise<unknown> {
    const body = await response.text();
    if (!body) {
      return null;
    }
    try {
      return JSON.parse(body);
    } catch {
      return body;
    }
  }

  private async refreshSession(): Promise<void> {
    if (!APIService.refreshPromise) {
      const rotate = async () => {
        const headers = new Headers();
        headers.set(APIService.BROWSER_HEADER, "1");
        const response = await fetch(this.buildUrl("/auth/refresh"), {
          method: "POST",
          headers,
          credentials: "include",
          cache: "no-store",
        });
        if (!response.ok) {
          throw new HttpError(response, await this.parseResponseBody(response));
        }
      };
      APIService.refreshPromise = rotate().finally(() => {
        APIService.refreshPromise = null;
      });
    }
    await APIService.refreshPromise;
  }

  private shouldRefresh(path: string, response: Response, config: IRequestConfig, retryAttempt: number): boolean {
    return (
      response.status === 401 &&
      response.headers.get(APIService.REJECTION_HEADER) === "1" &&
      config.authRefresh !== false &&
      config.skipAuthRefresh !== true &&
      retryAttempt === 0 &&
      path !== "/auth/refresh" &&
      path !== "/auth/sign-out"
    );
  }

  private async request<T>(
    method: HttpMethod,
    path: string,
    config: IRequestConfig = {},
    retryAttempt = 0
  ): Promise<IHttpResponse<T>> {
    const { params, headers, body, signal, credentials, validateStatus } = config;
    const requestHeaders = new Headers(headers);
    if (method !== "GET") {
      requestHeaders.set(APIService.BROWSER_HEADER, "1");
    }
    const init: RequestInit = {
      method,
      headers: requestHeaders,
      credentials: credentials ?? "include",
      cache: "no-store",
    };
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
    if (this.shouldRefresh(path, response, config, retryAttempt)) {
      // Gateway sets this marker only when it rejects before routing. A downstream
      // 401 or an uncertain network result never authorizes replay of a mutation.
      await this.refreshSession();
      return this.request<T>(method, path, config, 1);
    }
    const isOk = validateStatus ? validateStatus(response.status) : response.ok;
    if (!isOk) {
      throw new HttpError<T>(response, parsed as T | null);
    }
    return { data: parsed as T, status: response.status, headers: response.headers, raw: response };
  }

  protected get<T>(url: string, config: Omit<IRequestConfig, "body"> = {}): Promise<IHttpResponse<T>> {
    return this.request<T>("GET", url, config);
  }
  protected post<T>(url: string, body?: unknown, config: Omit<IRequestConfig, "body"> = {}): Promise<IHttpResponse<T>> {
    return this.request<T>("POST", url, { ...config, body });
  }
  protected put<T>(url: string, body?: unknown, config: Omit<IRequestConfig, "body"> = {}): Promise<IHttpResponse<T>> {
    return this.request<T>("PUT", url, { ...config, body });
  }
  protected patch<T>(
    url: string,
    body?: unknown,
    config: Omit<IRequestConfig, "body"> = {}
  ): Promise<IHttpResponse<T>> {
    return this.request<T>("PATCH", url, { ...config, body });
  }
  protected delete<T>(
    url: string,
    body?: unknown,
    config: Omit<IRequestConfig, "body"> = {}
  ): Promise<IHttpResponse<T>> {
    return this.request<T>("DELETE", url, { ...config, body });
  }
}
