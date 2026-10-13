import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
// constants
import { ADMIN_BASE_PATH, SITE_KEYWORDS } from "@syncturtle/constants";
// styles
import "@/styles/globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

const faviconVersion = "emerald-shell-v1";

export const metadata: Metadata = {
  title: "SyncTurtle Admin",
  description: "Manage your SyncTurtle instance.",
  metadataBase: new URL("https://app.syncturtle.com"),
  keywords: SITE_KEYWORDS,
  manifest: `${ADMIN_BASE_PATH}/site.webmanifest.json`,
  icons: {
    icon: [
      {
        url: `${ADMIN_BASE_PATH}/favicon/favicon.svg?v=${faviconVersion}`,
        type: "image/svg+xml",
        sizes: "any",
      },
      {
        url: `${ADMIN_BASE_PATH}/favicon/favicon-32x32.png?v=${faviconVersion}`,
        type: "image/png",
        sizes: "32x32",
      },
      {
        url: `${ADMIN_BASE_PATH}/favicon/favicon-16x16.png?v=${faviconVersion}`,
        type: "image/png",
        sizes: "16x16",
      },
    ],
    shortcut: `${ADMIN_BASE_PATH}/favicon/favicon.ico?v=${faviconVersion}`,
    apple: {
      url: `${ADMIN_BASE_PATH}/favicon/apple-touch-icon.png?v=${faviconVersion}`,
      sizes: "180x180",
      type: "image/png",
    },
  },
  appleWebApp: {
    capable: true,
    title: "SyncTurtle Admin",
    statusBarStyle: "default",
  },
};

export const viewport: Viewport = {
  themeColor: "#092C2B",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${geistSans.variable} ${geistMono.variable} antialiased`}>{children}</body>
    </html>
  );
}
