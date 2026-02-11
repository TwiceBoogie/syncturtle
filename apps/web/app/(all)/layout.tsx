import { Metadata, Viewport } from "next";

export const metadata: Metadata = {
  robots: {
    index: false,
    follow: false,
  },
};

export const viewport: Viewport = {
  minimumScale: 1,
  initialScale: 1,
  width: "device-width",
  viewportFit: "cover",
};

export default function AppLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
