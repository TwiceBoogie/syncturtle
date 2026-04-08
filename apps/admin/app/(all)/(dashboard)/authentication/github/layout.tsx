import type { Metadata } from "next";
import { ReactNode } from "react";

export const metadata: Metadata = {
  title: "GitHub Authentication - God Mode",
};

export default function GitHubAuthenticationLayout({ children }: { children: ReactNode }) {
  return <>{children}</>;
}
