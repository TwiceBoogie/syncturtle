import { Button } from "@heroui/react";
import Link from "next/link";

export default function Home() {
  return (
    <main className="min-h-[calc(100dvh-4rem)] flex items-center justify-center">
      <section className="mx-auto w-full max-w-5xl px-6 text-center">
        <p className="text-sm opacity-70">Self-hostable • Modular • Extensible</p>
        <h1 className="mt-4 text-5xl font-semibold tracking-tight">Syncturtle - The open-source Life OS</h1>
        <p className="mx-auto mt-5 max-w-2xl text-base opacity-80">
          Tasks, calendar, goals, passwords, and subscriptions in one place - extend with plugins
        </p>
        <div className="mt-8 flex items-center justify-center gap-3">
          <Link href={`/docs`}>
            <Button>Read the docs</Button>
          </Link>

          <a href="https://github.com/TwiceBoogie/syncturtle">
            <Button>View on Github</Button>
          </a>
        </div>
        <ul className="mx-auto mt-10 grid max-w-3xl grid-cols-1 gap-3 text-center sm:grid-cols-3">
          <li className="rounded-xl border border-white/10 p-4">
            <p className="font-medium">Self-hosted</p>
            <p className="mt-1 text-sm opacity-70">Run it on your own hardware.</p>
          </li>
          <li className="rounded-xl border border-white/10 p-4">
            <p className="font-medium">Modular</p>
            <p className="mt-1 text-sm opacity-70">Enable only what you need.</p>
          </li>
          <li className="rounded-xl border border-white/10 p-4">
            <p className="font-medium">Extensible</p>
            <p className="mt-1 text-sm opacity-70">Build features as plugins.</p>
          </li>
        </ul>
      </section>
    </main>
  );
}
