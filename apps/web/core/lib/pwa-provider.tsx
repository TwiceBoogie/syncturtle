"use client";

import { FC, useEffect } from "react";
import { SerwistProvider } from "@serwist/turbopack/react";
import { isClient } from "@syncturtle/utils";
import { IS_DEV } from "@/config";
import { env } from "@/env";

interface IPWAWrapper {
  children: React.ReactNode;
}

export const PWAProvider: FC<IPWAWrapper> = (props) => {
  const { children } = props;
  if (IS_DEV) {
    return <DisabledPWAProvider>{children}</DisabledPWAProvider>;
  }

  const basePath = env.NEXT_PUBLIC_BASE_PATH;
  const swUrl = `${basePath}/serwist/sw.js`;

  return <SerwistProvider swUrl={swUrl}>{children}</SerwistProvider>;
};

const DisabledPWAProvider: FC<IPWAWrapper> = (props) => {
  const { children } = props;
  useEffect(() => {
    if (isClient && "serviceWorker" in navigator) {
      navigator.serviceWorker
        .getRegistrations()
        .then((registrations) => {
          registrations.forEach((registration) => {
            registration.unregister().catch((error) => {
              console.error("Error unregistering service worker:", error);
            });
          });
        })
        .catch((error) => {
          console.error("Error unregistering service workers:", error);
        });
    }
  }, []);
  return <>{children}</>;
};
