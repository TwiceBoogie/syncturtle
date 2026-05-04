import { env } from "@/env";

export const IS_DEV = env.NODE_ENV === "development";
export const IS_PROD = env.NODE_ENV === "production";
