"use client";

import { ReactNode } from "react";

export default function RootLayout({ children }: { children: ReactNode }) {
  return <div className="relative z-10">{children}</div>;
}
