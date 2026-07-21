import type { ReactNode } from "react";
import type { Metadata, Viewport } from "next";

export const metadata: Metadata = {
  robots: {
    index: true,
    follow: false,
  },
};

export const viewport: Viewport = {
  minimumScale: 1,
  initialScale: 1,
  width: "device-width",
  viewportFit: "cover",
};

export default function HomeLayout({ children }: { children: ReactNode }) {
  return <>{children}</>;
}
