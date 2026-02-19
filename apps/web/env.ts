import { isClient, isServer, ObjectFromEntries, ObjectKeys } from "@syncturtle/utils";
import { createEnv } from "@t3-oss/env-nextjs";
import { CamelCase, concat, kebabCase, Replace, slice, length } from "string-ts";
import * as z from "zod";

export const CLIENT_ENV_PREFIX = "NEXT_PUBLIC_";
type ClientSchema = Record<`${typeof CLIENT_ENV_PREFIX}${string}`, z.ZodType>;

const coercedBoolean = z
  .string()
  .refine((s) => s === "true" || s === "false" || s === "0" || s === "1")
  .transform((s) => s === "true" || s === "1");
// const coercedNumber = z.coerce.number().int().positive();

// keep sorted
const clientSchema = {
  /**
   * The base path for this application
   */
  NEXT_PUBLIC_BASE_PATH: z
    .string()
    .regex(/^\/.*[^/]$/)
    .or(z.literal(""))
    .default(""),
  /**
   * The base url for the admin application
   */
  NEXT_PUBLIC_ADMIN_BASE_URL: z.url().optional(),
  /**
   * The base path for the admin application
   */
  NEXT_PUBLIC_ADMIN_BASE_PATH: z
    .string()
    .regex(/^\/.*[^/]$/)
    .or(z.literal(""))
    .default(""),
  /**
   * For posthog host url
   */
  NEXT_PUBLIC_POSTHOG_HOST: z.url().optional(),
  /**
   * Disable Microsoft Clarity analytics
   */
  NEXT_PUBLIC_ENABLE_SESSION_RECORDER: coercedBoolean.default(false),
  /**
   * Plausible domain
   */
  NEXT_PUBLIC_PLAUSIBLE_DOMAIN: z.string().optional(),
  NEXT_PUBLIC_CLARITY_ID: z.string().optional(),
} satisfies ClientSchema;

export const env = createEnv({
  server: {},
  shared: {
    NODE_ENV: z.enum(["development", "test", "production"]).default("development"),
  },
  client: clientSchema,
  experimental__runtimeEnv: {
    NODE_ENV: process.env.NODE_ENV,
    NEXT_PUBLIC_BASE_PATH: isServer ? process.env.NEXT_PUBLIC_BASE_PATH : getRuntimeEnvFromBody("basePath"),
    NEXT_PUBLIC_ADMIN_BASE_URL: isServer
      ? process.env.NEXT_PUBLIC_ADMIN_BASE_URL
      : getRuntimeEnvFromBody("adminBaseUrl"),
    NEXT_PUBLIC_ADMIN_BASE_PATH: isServer
      ? process.env.NEXT_PUBLIC_ADMIN_BASE_PATH
      : getRuntimeEnvFromBody("adminBasePath"),
    NEXT_PUBLIC_POSTHOG_HOST: isServer ? process.env.NEXT_PUBLIC_POSTHOG_HOST : getRuntimeEnvFromBody("posthogHost"),
    NEXT_PUBLIC_ENABLE_SESSION_RECORDER: isServer
      ? process.env.NEXT_PUBLIC_ENABLE_SESSION_RECORDER
      : getRuntimeEnvFromBody("enableSessionRecorder"),
    NEXT_PUBLIC_PLAUSIBLE_DOMAIN: isServer
      ? process.env.NEXT_PUBLIC_PLAUSIBLE_DOMAIN
      : getRuntimeEnvFromBody("plausibleDomain"),
    NEXT_PUBLIC_CLARITY_ID: isServer ? process.env.NEXT_PUBLIC_CLARITY_ID : getRuntimeEnvFromBody("clarityId"),
  },
  emptyStringAsUndefined: true,
});

type ClientEnvKey = keyof typeof clientSchema;
type DatasetKey = CamelCase<Replace<ClientEnvKey, typeof CLIENT_ENV_PREFIX>>;

/**
 * Browser-only function to get runtime env value from HTML body dataset.
 */
function getRuntimeEnvFromBody(key: DatasetKey) {
  if (typeof window === "undefined") {
    throw new TypeError("getRuntimeEnvFromBody can only be called in the browser");
  }

  const value = document.body.dataset[key];
  return value || undefined;
}

/**
 * Server-only function to get dataset map for embedding into the HTML body.
 */
export function getDatasetMap() {
  if (isClient) {
    throw new TypeError("getDatasetMap can only be called on the server");
  }
  return ObjectFromEntries(
    ObjectKeys(clientSchema).map((envKey) => [
      concat("data-", kebabCase(slice(envKey, length(CLIENT_ENV_PREFIX)))),
      env[envKey],
    ])
  );
}
