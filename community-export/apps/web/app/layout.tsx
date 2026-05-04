import type { Metadata, Viewport } from "next";
import Script from "next/script";

// styles
import "@/styles/globals.css";
import { AppProvider } from "./provider";
import { SITE_DESCRIPTION, SITE_KEYWORDS, SITE_TITLE } from "@syncturtle/constants";
import { env, getDatasetMap } from "@/env";

export const metadata: Metadata = {
  title: SITE_TITLE,
  description: SITE_DESCRIPTION,
  metadataBase: new URL("https://app.syncturtle.com"),
  keywords: SITE_KEYWORDS,
};

export const viewport: Viewport = {
  minimumScale: 1,
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
  width: "device-width",
  viewportFit: "cover",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  const isSessionRecorderEnabled = env.NEXT_PUBLIC_ENABLE_SESSION_RECORDER;
  const datasetMap = getDatasetMap();
  return (
    <html lang="en" dir="ltr" suppressHydrationWarning>
      <head>
        <meta name="theme-color" content="#fff" />
        <link rel="icon" type="image/png" sizes="32x32" href="/favicon/favicon-32x32.png" />
        <link rel="icon" type="image/png" sizes="16x16" href="/favicon/favicon-16x16.png" />
        <link rel="manifest" href="/site.webmanifest.json" />
        <link rel="shortcut icon" href="/favicon/favicon.ico" />
      </head>
      <body {...datasetMap}>
        <AppProvider>
          <div className="h-screen w-full overflow-hidden bg-custom-background-100 relative flex flex-col">
            <main className="w-full h-full overflow-hidden relative">{children}</main>
          </div>
        </AppProvider>
      </body>
      {env.NEXT_PUBLIC_PLAUSIBLE_DOMAIN && (
        <Script defer data-domain={env.NEXT_PUBLIC_PLAUSIBLE_DOMAIN} src="https://plausible.io/js/script.js" />
      )}
      {!!isSessionRecorderEnabled && env.NEXT_PUBLIC_CLARITY_ID && (
        <Script id="clarity-tracking" type="text/javascript">
          {`(function(c,l,a,r,i,t,y){
            c[a]=c[a]||function(){(c[a].q=c[a].q||[]).push(arguments)};
            t=l.createElement(r);t.async=1;t.src="https://www.clarity.ms/tag/"+i;
            y=l.getElementsByTagName(r)[0];y.parentNode.insertBefore(t,y);
          })(window, document, "clarity", "script", "${env.NEXT_PUBLIC_CLARITY_ID}");`}
        </Script>
      )}
    </html>
  );
}
