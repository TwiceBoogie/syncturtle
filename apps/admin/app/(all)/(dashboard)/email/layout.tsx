import type { Metadata } from "next";
import { ReactNode } from "react";

interface EmailLayoutProps {
  children: ReactNode;
}

export const metadata: Metadata = {
  title: "Email Settings - God Mode",
};

export default function EmailLayout({ children }: EmailLayoutProps) {
  return <>{children}</>;
}
