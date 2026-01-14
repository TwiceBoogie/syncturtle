"use client";

import { ThemeProvider } from "next-themes";
import { SWRConfig } from "swr";
// providers
import { StoreProvider } from "@/lib/store-context";
import { InstanceWrapper } from "@/lib/wrappers/instance-wrapper";
import { UserWrapper } from "@/lib/wrappers/user-wrapper";
// constants
import { DEFAULT_SWR_CONFIG } from "@syncturtle/constants";

export default function InstanceLayout({ children }: { children: React.ReactNode }) {
  return (
    <StoreProvider>
      <ThemeProvider
        attribute={"class"}
        defaultTheme="system"
        enableSystem
        enableColorScheme
        themes={["dark", "light"]}
      >
        <InstanceWrapper>
          <UserWrapper>
            <SWRConfig value={DEFAULT_SWR_CONFIG}>{children}</SWRConfig>
          </UserWrapper>
        </InstanceWrapper>
      </ThemeProvider>
    </StoreProvider>
  );
}
