"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronDown, Home, PanelLeftClose } from "lucide-react";
import { useWorkspace } from "@/hooks/store/use-workspace";
import { WorkspaceLogo } from "@/components/workspace/logo";

interface WorkspaceSidebarProps {
  workspaceSlug: string;
  onCollapse: () => void;
  onNavigate: () => void;
}

export function WorkspaceSidebar({ workspaceSlug, onCollapse, onNavigate }: WorkspaceSidebarProps) {
  const pathname = usePathname();
  const { getWorkspaceBySlug, workspaces } = useWorkspace();
  const workspace = getWorkspaceBySlug(workspaceSlug);
  const homePath = `/${workspaceSlug}`;
  const isHome = pathname === homePath || pathname === `${homePath}/`;

  return (
    <aside
      aria-label="Workspace navigation"
      className="relative flex h-full w-62.5 shrink-0 flex-col border-r border-custom-border-200 bg-custom-sidebar-background-100"
    >
      <div className="flex h-14 shrink-0 items-center gap-2 border-b border-custom-border-200 px-3">
        <details className="group min-w-0 flex-1">
          <summary
            className="flex cursor-pointer list-none items-center gap-2 rounded-md p-1.5 text-custom-text-100 hover:bg-custom-sidebar-background-80 focus-visible:outline-2 focus-visible:outline-custom-primary-100 [&::-webkit-details-marker]:hidden"
            aria-label="Switch Space"
          >
            <WorkspaceLogo logo={workspace?.logoUrl} name={workspace?.name ?? workspaceSlug} />
            <span className="min-w-0 flex-1 truncate text-sm font-semibold">{workspace?.name ?? workspaceSlug}</span>
            <ChevronDown aria-hidden="true" className="size-4 shrink-0 text-custom-text-300" />
          </summary>
          <div className="absolute left-3 right-3 z-30 mt-1 rounded-lg border border-custom-border-200 bg-custom-background-100 p-1 shadow-lg">
            <p className="px-2 py-1 text-xs font-medium text-custom-text-300">Your Spaces</p>
            {Object.values(workspaces).map((item) => (
              <Link
                key={item.id}
                href={`/${item.slug}`}
                onClick={onNavigate}
                className="block truncate rounded-md px-2 py-2 text-sm text-custom-text-200 hover:bg-custom-background-80 focus-visible:outline-2 focus-visible:outline-custom-primary-100"
              >
                {item.name}
              </Link>
            ))}
            <Link
              href="/create-workspace"
              onClick={onNavigate}
              className="block rounded-md px-2 py-2 text-sm text-custom-text-200 hover:bg-custom-background-80 focus-visible:outline-2 focus-visible:outline-custom-primary-100"
            >
              Create a Space
            </Link>
          </div>
        </details>
        <button
          type="button"
          onClick={onCollapse}
          aria-label="Hide workspace sidebar"
          title="Hide workspace sidebar"
          className="grid size-8 shrink-0 place-items-center rounded-md text-custom-text-300 hover:bg-custom-sidebar-background-80 hover:text-custom-text-100 focus-visible:outline-2 focus-visible:outline-custom-primary-100"
        >
          <PanelLeftClose aria-hidden="true" className="size-4" />
        </button>
      </div>
      <nav aria-label="Space pages" className="flex-1 px-3 py-4">
        <p className="px-2 pb-2 text-xs font-semibold uppercase tracking-wide text-custom-sidebar-text-400">Space</p>
        <Link
          href={homePath}
          aria-current={isHome ? "page" : undefined}
          onClick={onNavigate}
          className={`flex items-center gap-3 rounded-md px-2.5 py-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-custom-primary-100 ${isHome ? "bg-custom-primary-100/10 text-custom-primary-100" : "text-custom-sidebar-text-200 hover:bg-custom-sidebar-background-80"}`}
        >
          <Home aria-hidden="true" className="size-4" /> Home
        </Link>
      </nav>
      <p className="border-t border-custom-border-200 px-5 py-4 text-xs text-custom-sidebar-text-400">
        More of your Space is coming soon.
      </p>
    </aside>
  );
}
