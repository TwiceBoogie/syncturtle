"use client";

import { ThemeProvider } from "next-themes";
import { SWRConfig } from "swr";
// providers
import { StoreProvider } from "@/lib/store-context";
import { InstanceWrapper } from "@/lib/wrappers/instance-wrapper";
import { UserWrapper } from "@/lib/wrappers/user-wrapper";
// constants
import { DEFAULT_SWR_CONFIG } from "@syncturtle/constants";
import { Toast } from "@heroui/react";

export default function InstanceLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <Toast.Provider />
      <StoreProvider>
        <ThemeProvider themes={["light", "dark"]} defaultTheme="system" enableSystem>
          <SWRConfig value={DEFAULT_SWR_CONFIG}>
            <InstanceWrapper>
              <UserWrapper>{children}</UserWrapper>
            </InstanceWrapper>
          </SWRConfig>
        </ThemeProvider>
      </StoreProvider>
    </>
  );
}
