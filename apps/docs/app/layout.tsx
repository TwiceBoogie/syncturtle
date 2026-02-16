import type { Metadata } from "next";
// styles
import "@/styles/globals.css";
import { Layout, Navbar } from "nextra-theme-docs";
import { getPageMap } from "nextra/page-map";
import { AppFooter } from "@/components/core/footer";
import { Banner } from "nextra/components";

export const metadata: Metadata = {
  title: "Syncturtle | Docs",
};

const navbar = (
  <Navbar
    logo={
      <b>
        Sync<span className="text-green-400">turtle</span>
      </b>
    }
    projectLink="https://github.com/TwiceBoogie/syncturtle"
  />
);
const banner = <Banner storageKey="syncturtle-banner">Docs are a work in progress</Banner>;

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" dir="ltr" suppressHydrationWarning>
      <body className="min-h-dvh">
        <Layout
          navbar={navbar}
          pageMap={await getPageMap()}
          banner={banner}
          footer={<AppFooter />}
          nextThemes={{
            attribute: "class",
            defaultTheme: "system",
            disableTransitionOnChange: true,
            storageKey: "theme",
          }}
        >
          {children}
        </Layout>
      </body>
    </html>
  );
}
