/// <reference lib="esnext" />
/// <reference lib="webworker" />

import { Serwist, type PrecacheEntry, type SerwistGlobalConfig } from "serwist";
import { defaultCache } from "@serwist/turbopack/worker";
import { withLeadingSlash } from "ufo";

declare global {
  interface WorkerGlobalScope extends SerwistGlobalConfig {
    __SW_MANIFEST: (PrecacheEntry | string)[] | undefined;
  }
}

declare const self: ServiceWorkerGlobalScope;

const scopePathname = new URL(self.registration.scope).pathname;
const basePath = scopePathname.replace(/\/serwist\/$/, "").replace(/\/$/, "");
const offlineUrl = `${basePath}/_offline.html`;
const adminBasePath = (process.env.NEXT_PUBLIC_ADMIN_BASE_PATH || "/god-mode").replace(/\/+$/, "");

const normalizeManifestUrl = (url: string): string => {
  if (url.startsWith("/serwist/")) return url.replace(/^\/serwist\//, "/");

  return withLeadingSlash(url);
};

const manifest: (PrecacheEntry | string)[] = (self.__SW_MANIFEST ?? []).map((entry): PrecacheEntry | string => {
  if (typeof entry === "string") return normalizeManifestUrl(entry);

  return {
    ...entry,
    url: normalizeManifestUrl(entry.url),
  };
});

const serwist = new Serwist({
  precacheEntries: manifest,
  skipWaiting: true,
  disableDevLogs: true,
  clientsClaim: true,
  navigationPreload: true,
  runtimeCaching: [
    {
      // The root-scoped Web worker also sees Admin pages and their root-level API calls.
      // Keep those requests out of Web caches and its offline document fallback.
      matcher: ({ sameOrigin, url: { pathname } }) =>
        sameOrigin &&
        (pathname === adminBasePath ||
          pathname.startsWith(`${adminBasePath}/`) ||
          pathname === "/api/instances" ||
          pathname.startsWith("/api/instances/") ||
          pathname === "/api/users/me" ||
          pathname.startsWith("/api/users/me/") ||
          pathname === "/api/get-csrf-token"),
      // A plain handler has no Serwist Strategy fallback plugin.
      handler: ({ request }) => fetch(request),
    },
    ...defaultCache,
  ],
  fallbacks: {
    entries: [
      {
        url: offlineUrl,
        matcher({ request }) {
          return request.destination === "document";
        },
      },
    ],
  },
});

serwist.addEventListeners();
