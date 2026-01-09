"use client";

import { FC, ReactNode } from "react";
import dynamic from "next/dynamic";
import { StoreProvider } from "@/lib/store-context";
import { ProgressProvider } from "@bprogress/next/app";
import { TranslationProvider } from "@syncturtle/i18n";
import { ThemeProvider } from "next-themes";
// dynamic imports
const StoreWrapper = dynamic(() => import("@/lib/wrappers/store-wrapper"), { ssr: false });

export interface IAppProvider {
  children: ReactNode;
}

export const AppProvider: FC<IAppProvider> = (props) => {
  const { children } = props;

  return (
    <ProgressProvider>
      <StoreProvider>
        <ThemeProvider>
          <StoreWrapper>
            <TranslationProvider>{children}</TranslationProvider>
          </StoreWrapper>
        </ThemeProvider>
      </StoreProvider>
    </ProgressProvider>
  );
};
