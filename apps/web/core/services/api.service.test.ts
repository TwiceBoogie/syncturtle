import { afterEach, describe, expect, it, vi } from "vitest";
import { APIService, HttpError } from "./api.service";

const rejected = () =>
  new Response(null, {
    status: 401,
    headers: { "X-ST-Gateway-Auth-Rejection": "1" },
  });
const success = () => new Response(JSON.stringify({ ok: true }), { status: 200 });

class TestService extends APIService {
  constructor() {
    super("https://api.example.test");
  }
  read(config: { authRefresh?: boolean; skipAuthRefresh?: boolean } = {}) {
    return this.get<{ ok: boolean }>("/api/protected", config);
  }
  write() {
    return this.post<{ ok: boolean }>("/api/protected", { value: 1 });
  }
  patchValue() {
    return this.patch<void>("/api/protected", { value: 1 });
  }
  putValue() {
    return this.put<void>("/api/protected", { value: 1 });
  }
  deleteValue() {
    return this.delete<void>("/api/protected");
  }
}

afterEach(() => vi.unstubAllGlobals());

describe("simple browser authentication transport", () => {
  it("uses credentials and the fixed browser header on every unsafe method", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async () => new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new TestService();
    await service.write();
    await service.patchValue();
    await service.putValue();
    await service.deleteValue();
    expect(fetchMock.mock.calls.map(([, request]) => request?.method)).toEqual(["POST", "PATCH", "PUT", "DELETE"]);
    for (const [, request] of fetchMock.mock.calls) {
      expect(request?.credentials).toBe("include");
      expect(new Headers(request?.headers).get("X-ST-Browser-Request")).toBe("1");
    }
  });

  it("rotates once and replays a Gateway-rejected protected write once", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(rejected())
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(success());
    vi.stubGlobal("fetch", fetchMock);
    expect((await new TestService().write()).data.ok).toBe(true);
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(String(fetchMock.mock.calls[1]?.[0])).toBe("https://api.example.test/auth/refresh");
    expect(new Headers(fetchMock.mock.calls[1]?.[1]?.headers).get("X-ST-Browser-Request")).toBe("1");
    expect(fetchMock.mock.calls[2]?.[1]?.method).toBe("POST");
  });

  it("refreshes a protected GET without requiring every service caller to opt in", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(rejected())
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(success());
    vi.stubGlobal("fetch", fetchMock);
    expect((await new TestService().read()).data.ok).toBe(true);
    expect(fetchMock.mock.calls.map(([, request]) => request?.method)).toEqual(["GET", "POST", "GET"]);
  });

  it("respects a public opt-out and does not replay a downstream 401", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(rejected())
      .mockResolvedValueOnce(new Response(null, { status: 401 }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new TestService();
    await expect(service.read({ authRefresh: false })).rejects.toBeInstanceOf(HttpError);
    await expect(service.write()).rejects.toBeInstanceOf(HttpError);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it("respects the bootstrap skipAuthRefresh opt-out", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValueOnce(rejected());
    vi.stubGlobal("fetch", fetchMock);
    await expect(new TestService().read({ skipAuthRefresh: true })).rejects.toMatchObject({ status: 401 });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("stops after one retry even if Gateway rejects again", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(rejected())
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(rejected());
    vi.stubGlobal("fetch", fetchMock);
    await expect(new TestService().read()).rejects.toMatchObject({ status: 401 });
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it("reports stale refresh and Redis outage without retrying the protected call", async () => {
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(rejected())
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(rejected())
      .mockResolvedValueOnce(new Response(null, { status: 503 }));
    vi.stubGlobal("fetch", fetchMock);
    const service = new TestService();
    await expect(service.read()).rejects.toMatchObject({ status: 401 });
    await expect(service.read()).rejects.toMatchObject({ status: 503 });
    expect(fetchMock).toHaveBeenCalledTimes(4);
  });

  it("joins two refreshes in the same runtime", async () => {
    let releaseRefresh!: (response: Response) => void;
    let refreshStarted!: () => void;
    const started = new Promise<void>((resolve) => {
      refreshStarted = resolve;
    });
    let protectedCalls = 0;
    const fetchMock = vi.fn<typeof fetch>().mockImplementation(async (url) => {
      if (String(url).endsWith("/auth/refresh")) {
        refreshStarted();
        return new Promise<Response>((resolve) => {
          releaseRefresh = resolve;
        });
      }
      protectedCalls += 1;
      return protectedCalls <= 2 ? rejected() : success();
    });
    vi.stubGlobal("fetch", fetchMock);
    const service = new TestService();
    const first = service.read();
    const second = service.read();
    await started;
    releaseRefresh(new Response(null, { status: 204 }));
    expect((await first).data.ok).toBe(true);
    expect((await second).data.ok).toBe(true);
    expect(fetchMock.mock.calls.filter(([url]) => String(url).endsWith("/auth/refresh"))).toHaveLength(1);
  });

  it("does not retry after an uncertain network result", async () => {
    const fetchMock = vi.fn<typeof fetch>().mockRejectedValueOnce(new TypeError("connection lost"));
    vi.stubGlobal("fetch", fetchMock);
    await expect(new TestService().write()).rejects.toThrow("connection lost");
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });
});
