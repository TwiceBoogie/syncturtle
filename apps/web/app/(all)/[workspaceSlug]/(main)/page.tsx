"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { ArrowUpRight, UsersRound } from "lucide-react";
import { useUser } from "@/hooks/store/user";
import { useWorkspace } from "@/hooks/store/use-workspace";

export default function WorkspaceDashboardPage() {
  const { workspaceSlug } = useParams<{ workspaceSlug: string }>();
  const { data: user } = useUser();
  const { getWorkspaceBySlug } = useWorkspace();
  const workspace = getWorkspaceBySlug(workspaceSlug ?? "");
  const firstName = user?.firstName || user?.displayName?.split(" ")[0];

  return (
    <div className="mx-auto w-full max-w-5xl px-6 py-8 md:px-10 md:py-10">
      <div className="mb-10">
        <p className="mb-3 text-xs font-semibold uppercase tracking-widest text-custom-text-400">Space home</p>
        <h1 className="text-3xl font-semibold tracking-tight text-custom-text-100">
          Welcome{firstName ? `, ${firstName}` : ""}
        </h1>
        <p className="mt-2 text-sm text-custom-text-300">Your place to organize what matters together.</p>
      </div>
      <section
        aria-labelledby="space-overview-title"
        className="rounded-xl border border-custom-border-200 bg-custom-sidebar-background-100 p-6 md:p-8"
      >
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-medium uppercase tracking-wide text-custom-text-400">Current Space</p>
            <h2 id="space-overview-title" className="mt-2 text-2xl font-semibold text-custom-text-100">
              {workspace?.name ?? workspaceSlug}
            </h2>
            <p className="mt-1 text-sm text-custom-text-300">A shared home for your plans and everyday life.</p>
          </div>
          {workspace && (
            <div className="flex items-center gap-2 rounded-lg bg-custom-background-100 px-3 py-2 text-sm text-custom-text-200">
              <UsersRound aria-hidden="true" className="size-4" />
              {workspace.totalMembers} {workspace.totalMembers === 1 ? "member" : "members"}
            </div>
          )}
        </div>
        <div className="mt-8 grid gap-4 border-t border-custom-border-200 pt-6 text-sm sm:grid-cols-2">
          <div>
            <p className="text-custom-text-400">Space address</p>
            <p className="mt-1 font-medium text-custom-text-200">/{workspace?.slug ?? workspaceSlug}</p>
          </div>
          <div>
            <p className="text-custom-text-400">Space owner</p>
            <p className="mt-1 font-medium text-custom-text-200">{workspace?.owner?.displayName || "—"}</p>
          </div>
        </div>
      </section>
      <section aria-labelledby="getting-started-title" className="mt-8">
        <h2 id="getting-started-title" className="text-lg font-semibold text-custom-text-100">
          Get started
        </h2>
        <p className="mt-1 text-sm text-custom-text-300">
          Manage your profile or move between your Spaces while more ways to organize arrive.
        </p>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          <Link
            href="/profile"
            className="flex items-center justify-between rounded-lg border border-custom-border-200 bg-custom-sidebar-background-100 p-4 text-sm font-medium text-custom-text-200 hover:border-custom-primary-100 focus-visible:outline-2 focus-visible:outline-custom-primary-100"
          >
            Your profile
            <ArrowUpRight aria-hidden="true" className="size-4" />
          </Link>
          <Link
            href="/create-workspace"
            className="flex items-center justify-between rounded-lg border border-custom-border-200 bg-custom-sidebar-background-100 p-4 text-sm font-medium text-custom-text-200 hover:border-custom-primary-100 focus-visible:outline-2 focus-visible:outline-custom-primary-100"
          >
            Create a Space
            <ArrowUpRight aria-hidden="true" className="size-4" />
          </Link>
        </div>
      </section>
    </div>
  );
}
