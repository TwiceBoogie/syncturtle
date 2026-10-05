import { CsrfTokenCache, revokedCurrentSession, shouldRetryInvalidCsrf } from "./csrf-token-cache";
import { BrowserSessionCoordinator } from "@syncturtle/utils";
import type { ICsrfTokenData } from "@syncturtle/types";
import type { BrowserSessionEnvironment } from "@syncturtle/utils";

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
  /** Shared by the bounded retries of one protected operation */
  _authConfirmation?: { attempted: boolean };
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
  private static refreshConfirmation: { attempted: boolean } | null = null;
  private static coordinator: BrowserSessionCoordinator | null = null;
  private static coordinatorAttempt = false;
  private static authenticationRevision = 0;
  private static confirmationPromise: Promise<boolean> | null = null;
  private static readonly signedOutListeners = new Set<() => void>();

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  private static sessionCoordinator(): BrowserSessionCoordinator | null {
    if (typeof window === "undefined") return null;

    if (APIService.coordinatorAttempt) return APIService.coordinator;

    APIService.coordinatorAttempt = true;
    try {
      const environment: { -readonly [K in keyof BrowserSessionEnvironment]: BrowserSessionEnvironment[K] } = {
        runtimeId: window.crypto.randomUUID(),
        now: Date.now,
        schedule: (callback, milliseconds) => {
          const timer = window.setTimeout(callback, milliseconds);
          return () => window.clearTimeout(timer);
        },
        openChannel: (name, receive) => {
          const channel = new BroadcastChannel(name);
          channel.onmessage = (event: MessageEvent<unknown>) => receive(event.data);
          return {
            postMessage: (event) => channel.postMessage(event),
            close: () => {
              channel.onmessage = null;
              channel.close();
            },
          };
        },
      };

      try {
        environment.storage = window.localStorage;
      } catch {
        // Storage-denied browsers retain the bounded app-owned fallback.
      }

      try {
        const locks = window.navigator.locks;
        if (locks) {
          environment.locks = {
            request: async (name, options, action) => {
              await locks.request(name, options, action);
            },
          };
        }
      } catch {
        // Missing locks do not prevent an ordinary request or one fallback.
      }

      const coordinator = new BrowserSessionCoordinator(environment, 2_000);
      coordinator.subscribe((event) => {
        if (event.kind === "CSRF_REPLACED") APIService.csrfCache.invalidate();
      });

      APIService.coordinator = coordinator;
      window.addEventListener("pagehide", (event) => {
        // A bfcache page retains its runtime; a destroyed page closes its channel.
        if (!event.persisted) coordinator.dispose();
      });
    } catch {
      // No random UUID/browser support: use the existing runtime-only bound.
    }

    return APIService.coordinator;
  }

  static clearSessionCsrf(): void {
    APIService.csrfCache.invalidate();
  }

  protected announceAuthStateChanged(): void {
    APIService.sessionCoordinator()?.notifyAuthStateChanged();
  }

  /** One wrapper subscription; cleanup removes both local and remote listeners. */
  subscribeAuthentication(signedOut: () => void): () => void {
    APIService.signedOutListeners.add(signedOut);
    const unsubscribe = APIService.sessionCoordinator()?.subscribe((event) => {
      if (event.kind !== "AUTH_STATE_CHANGED") return;
      APIService.csrfCache.invalidate();
      // Duplicate in-flight notices share this check. Never rebroadcast a remote notice.
      void this.confirmAuthentication(false).catch(() => {});
    });
    return () => {
      APIService.signedOutListeners.delete(signedOut);
      unsubscribe?.();
    };
  }

  /** No refresh, CSRF, store hydration, or recursive recovery in this probe. */
  protected confirmAuthentication(announce: boolean): Promise<boolean> {
    if (!APIService.confirmationPromise) {
      const coordinator = APIService.sessionCoordinator();
      const generation = coordinator?.observeGeneration();
      const revision = APIService.authenticationRevision;
      APIService.confirmationPromise = this.get("/api/users/me", {
        skipAuthRefresh: true,
        csrf: false,
        validateStatus: (status) => status === 200 || status === 401,
      })
        .then((response) => {
          if (response.status === 200) return true;
          if (revision !== APIService.authenticationRevision || generation !== coordinator?.observeGeneration()) {
            // The check itself became stale. Do not start another check or claim sign-out.
            return false;
          }
          APIService.csrfCache.invalidate();
          for (const listener of APIService.signedOutListeners) listener();
          if (announce) this.announceAuthStateChanged();
          return false;
        })
        .finally(() => {
          APIService.confirmationPromise = null;
        });
    }
    return APIService.confirmationPromise;
  }

  protected requestCsrfToken(): Promise<ICsrfTokenData> {
    const coordinator = APIService.sessionCoordinator();
    return APIService.csrfCache.get(async () => {
      const response = await this.get<ICsrfTokenData>("/api/get-csrf-token", { csrf: false, skipAuthRefresh: true });
      coordinator?.notifyCsrfReplaced();
      return response.data;
    });
  }

  protected invalidateCsrfToken(): void {
    APIService.csrfCache.invalidate();
  }

  private async refreshSession(confirmation: { attempted: boolean }): Promise<void> {
    const coordinator = APIService.sessionCoordinator();
    const observedGeneration = coordinator?.observeGeneration() ?? 0;
    if (!APIService.refreshPromise) {
      APIService.refreshConfirmation = confirmation;
      const refresh = async () => {
        await this.request<void>("POST", "/auth/refresh", {
          skipAuthRefresh: true,
          validateStatus: (status) => status === 200 || status === 204,
        });
      };
      APIService.refreshPromise = (async () => {
        try {
          const result = await coordinator?.coordinateRefresh(observedGeneration, refresh);
          if (!result || result.status === "fallback") {
            await refresh();
            coordinator?.recordRefreshSuccess();
          } else if (result.status === "disposed") {
            throw new Error("Browser session coordinator disposed");
          }
        } catch (error) {
          if (!(error instanceof HttpError) || (error.status !== 401 && error.status !== 403)) throw error;
          if (coordinator && coordinator.observeGeneration() > observedGeneration) return;
          confirmation.attempted = true;
          if (await this.confirmAuthentication(true)) return;
          throw error;
        }
      })().finally(() => {
        APIService.refreshPromise = null;
      });
    }
    // A joined operation must not introduce a second confirmation after retry.
    const result = APIService.refreshPromise;
    const sharedConfirmation = APIService.refreshConfirmation;
    await result;
    confirmation.attempted = sharedConfirmation?.attempted ?? false;
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
    APIService.sessionCoordinator();
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
      const confirmation = config._authConfirmation ?? { attempted: false };
      await this.refreshSession(confirmation);
      return this.request<T>(method, path, {
        ...config,
        _retryAttempt: 1,
        _authConfirmation: confirmation,
      });
    }

    if (
      response.status === 401 &&
      config.authRefresh === true &&
      config.skipAuthRefresh !== true &&
      config._retryAttempt === 1 &&
      !config._authConfirmation?.attempted
    ) {
      const confirmation = config._authConfirmation ?? { attempted: false };
      confirmation.attempted = true;
      await this.confirmAuthentication(true);
      // Regardless of the probe, there is no second protected retry.
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

    if (
      response.ok &&
      (config.authRefresh === true ||
        path === "/auth/refresh" ||
        path === "/auth/set-password" ||
        path === "/api/users/me")
    ) {
      APIService.authenticationRevision += 1;
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
