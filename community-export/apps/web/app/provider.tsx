"use client";

import { FC, ReactNode } from "react";
import dynamic from "next/dynamic";
import { StoreProvider } from "@/lib/store-context";
import { ProgressProvider } from "@bprogress/next/app";
import { TranslationProvider } from "@syncturtle/i18n";
import { ThemeProvider } from "next-themes";
import { SWRConfig } from "swr";
import { WEB_SWR_CONFIG } from "@syncturtle/constants";
import { InstanceWrapper } from "@/lib/wrappers/instance-wrapper";
import { PWAProvider } from "@/lib/pwa-provider";
// dynamic imports
const StoreWrapper = dynamic(() => import("@/lib/wrappers/store-wrapper"), { ssr: false });

export interface IAppProvider {
  children: ReactNode;
}

export const AppProvider: FC<IAppProvider> = (props) => {
  const { children } = props;

  return (
    <ProgressProvider height="4px" options={{ showSpinner: false }} shallowRouting>
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
