import Link from "next/link";
import { Footer } from "nextra-theme-docs";

export const AppFooter = () => (
  <Footer>
    <div className="mx-auto w-full max-w-5xl px-6 py-10">
      <div className="grid gap-8 sm:grid-cols-3">
        <div>
          <p className="text-sm font-medium">For recruiters</p>
          <ul className="mt-3 space-y-2 text-sm opacity-80">
            <li>
              <Link href={`/docs/architecture`}>Architecture overview</Link>
            </li>
            <li>
              <Link href={`/docs/services`}>Service map</Link>
            </li>
            <li>
              <Link href={`/docs/testing`}>Testing strategy</Link>
            </li>
            <li>
              <Link href={`docs/self-hosting`}>Run locally</Link>
            </li>
          </ul>
        </div>
        <div>
          <p className="text-sm font-medium">Project</p>
          <ul className="mt-3 space-y-2 text-sm opacity-80">
            <li>
              <Link href={`/docs`}>Documentation</Link>
            </li>
            <li>
              <Link href={`/docs/adr`}>ADRs</Link>
            </li>
            <li>
              <Link href={`/docs/roadmap`}>Roadmap</Link>
            </li>
            <li>
              <Link href={`/docs/contributing`}>Contributing</Link>
            </li>
          </ul>
        </div>
        <div>
          <p className="text-sm font-medium">Links</p>
          <ul className="mt-3 space-y-2 text-sm opacity-80">
            <li>
              <a href="https://github.com/TwiceBoogie/syncturtle" target="_blank" rel="noreferrer">
                Github
              </a>
            </li>
          </ul>
        </div>
      </div>
      <div className="mt-10 flex flex-col gap-2 border-t border-black/10 pt-4 text-xs opacity-70 dark:border-white/10 sm:flex-row sm:items-center sm:justify-between">
        <span>© {new Date().getFullYear()} SyncTurtle</span>
        <span>Spring Boot microservices • Maven multi-module • Turbo/Next.js • Docker/K8s</span>
      </div>
    </div>
  </Footer>
);
