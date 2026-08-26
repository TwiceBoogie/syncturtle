"use client";

import type { FC, ReactNode } from "react";
import dynamic from "next/dynamic";
import { ProgressProvider } from "@bprogress/next/app";
import { ThemeProvider } from "next-themes";
import { SWRConfig } from "swr";
import { TranslationProvider } from "@syncturtle/i18n";
import { WEB_SWR_CONFIG } from "@syncturtle/constants";
// store provider
import { StoreProvider } from "@/lib/store-context";
// wrappers
import { InstanceWrapper } from "@/lib/wrappers/instance-wrapper";
import { PWAProvider } from "@/lib/pwa-provider";
import { Toast } from "@heroui/react";
// dynamic imports
const StoreWrapper = dynamic(() => import("@/lib/wrappers/store-wrapper"), { ssr: false });

export interface IAppProvider {
  children: ReactNode;
}

export const AppProvider: FC<IAppProvider> = (props) => {
  const { children } = props;

  return (
    <ProgressProvider height="4px" options={{ showSpinner: false }} shallowRouting>
      <Toast.Provider />
      <PWAProvider>
        <StoreProvider>
          <ThemeProvider themes={["light", "dark"]} defaultTheme="system" enableSystem>
            <TranslationProvider>
              <StoreWrapper>
                <InstanceWrapper>
                  <SWRConfig value={WEB_SWR_CONFIG}>{children}</SWRConfig>
                </InstanceWrapper>
              </StoreWrapper>
            </TranslationProvider>
          </ThemeProvider>
        </StoreProvider>
      </PWAProvider>
    </ProgressProvider>
  );
};
