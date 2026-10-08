import { useState, type FC } from "react";
import { useParams } from "next/navigation";
import { Check, ChevronDown, CirclePlus, LogOut, Mail, Settings, UserPlus } from "lucide-react";
// heroui
import { Dropdown, Label, Separator } from "@heroui/react";
// utils
import { cn } from "@syncturtle/ui";
// components
import { WorkspaceLogo } from "../logo";

interface IWorkspaceMenuRootProps {
  variant: "sidebar" | "top-navigation";
}

type DemoWorkspace = {
  id: string;
  name: string;
  slug: string;
  role: string;
  memberCount: number;
  plan?: string;
};

const defaultWorkspace: DemoWorkspace = {
  id: "personal",
  name: "Personal",
  slug: "personal",
  role: "Owner",
  memberCount: 1,
};

const workspaces: DemoWorkspace[] = [
  defaultWorkspace,
  {
    id: "family",
    name: "Family",
    slug: "family",
    role: "Member",
    memberCount: 4,
    plan: "Free",
  },
];

export const WorkspaceMenuRoot: FC<IWorkspaceMenuRootProps> = () => {
  const { workspaceSlug } = useParams<{ workspaceSlug: string }>();
  // state
  const [isOpen, setIsOpen] = useState(false);
  // derived values
  const active = workspaces.find((workspace) => workspace.slug === workspaceSlug) ?? defaultWorkspace;
  const inactive = workspaces.filter((workspace) => workspace.id !== active.id);
  const memberLabel = (count: number) => `${count} ${count === 1 ? "member" : "members"}`;

  return (
    <div className="relative h-full w-fit max-w-48 min-w-0">
      <Dropdown isOpen={isOpen} onOpenChange={setIsOpen}>
        <Dropdown.Trigger
          aria-label={`Switch workspace; current workspace: ${active.name}`}
          className={cn(
            "flex h-[32px] max-w-[192px] min-w-0 items-center justify-between gap-[4px]",
            "rounded-sm p-[4px] text-left text-custom-text-200",
            "hover:bg-custom-background-90",
            "data-focus-visible:ring-2 data-focus-visible:ring-custom-primary-100",
            isOpen && "bg-custom-sidebar-background-90"
          )}
        >
          <span className="flex min-w-0 items-center gap-[8px]">
            <WorkspaceLogo
              logo={null}
              name={active.name}
              classNames="size-[24px] shrink-0 rounded-sm border border-custom-border-200"
            />
            <span className="min-w-0 truncate text-[14px] font-medium leading-[21.5px] text-custom-text-100">
              {active.name}
            </span>
          </span>
          <ChevronDown
            aria-hidden="true"
            className={`size-[16px] shrink-0 text-custom-text-400 transition-transform duration-150 ${
              isOpen ? "rotate-180" : ""
            }`}
          />
        </Dropdown.Trigger>
        <Dropdown.Popover
          placement="bottom start"
          offset={6}
          className={[
            "w-[322px] max-w-[calc(100vw-2rem)] overflow-hidden",
            "rounded-lg border border-custom-border-200",
            "bg-custom-background-90 p-0",
            "shadow-[0_10px_10px_-10px_rgba(0,0,0,0.2),0_30px_60px_-12px_rgba(0,0,0,0.3)]",
          ].join(" ")}
        >
          <Dropdown.Menu
            aria-label="Workspaces and account actions"
            className="max-h-[min(24rem,calc(100vh-3rem))] overflow-y-auto p-1"
          >
            <Dropdown.Section
              className={[
                "grid grid-cols-[max-content_max-content_1fr]",
                "items-center gap-x-2 gap-y-2.5 rounded-md p-2",
                "bg-black/4 dark:bg-white/[0.07]",
              ].join(" ")}
            >
              <Dropdown.Item
                id={`workspace-${active.id}`}
                textValue={active.name}
                href={`/${active.slug}`}
                aria-current="page"
                className={[
                  "col-span-3 flex h-[38px] min-w-0 items-center gap-2",
                  "rounded-md bg-transparent p-0 text-custom-text-100",
                  "data-focus-visible:ring-2 data-focus-visible:ring-custom-primary-100",
                ].join(" ")}
              >
                <WorkspaceLogo
                  logo={null}
                  name={active.name}
                  classNames="size-8 shrink-0 rounded-md border border-custom-border-200"
                />
                <span className="flex min-w-0 flex-1 flex-col">
                  <Label className="truncate text-[13px] leading-5">{active.name}</Label>
                  <span className="truncate text-xs leading-[14px] text-custom-text-300">
                    {active.role} · {memberLabel(active.memberCount)}
                  </span>
                </span>
                <Check aria-hidden="true" className="size-4 shrink-0" />
              </Dropdown.Item>
              <Dropdown.Item
                id="workspace-settings"
                textValue="Settings"
                onAction={() => setIsOpen(false)}
                className="flex h-6 items-center gap-1 rounded-md border border-custom-border-300 bg-custom-background-90 px-2 text-[13px] text-custom-text-200 data-hovered:bg-custom-background-80"
              >
                <Settings aria-hidden="true" className="size-3.5" />
                <Label>Settings</Label>
              </Dropdown.Item>
              <Dropdown.Item
                id="invite-members"
                textValue="Invite members"
                onAction={() => setIsOpen(false)}
                className="flex h-6 items-center gap-1 rounded-md border border-custom-border-300 bg-custom-background-90 px-2 text-[13px] text-custom-text-200 data-hovered:bg-custom-background-80"
              >
                <UserPlus aria-hidden="true" className="size-3.5" />
                <Label>Invite members</Label>
              </Dropdown.Item>
            </Dropdown.Section>
            {inactive.map((workspace) => (
              <Dropdown.Item
                key={workspace.id}
                id={`workspace-${workspace.id}`}
                textValue={workspace.name}
                href={`/${workspace.slug}`}
                className={[
                  "flex h-15 min-w-0 items-center gap-2 rounded-md p-2",
                  "text-custom-text-100",
                  "data-hovered:bg-black/4 dark:data-hovered:bg-white/5",
                  "data-focus-visible:ring-2 data-focus-visible:ring-custom-primary-100",
                ].join(" ")}
              >
                <WorkspaceLogo
                  logo={null}
                  name={workspace.name}
                  classNames="size-8 shrink-0 rounded-md border border-custom-border-200"
                />
                <span className="flex min-w-0 flex-1 flex-col">
                  <Label className="truncate text-[13px] leading-5">{workspace.name}</Label>
                  <span className="truncate text-xs leading-[14px] text-custom-text-300">
                    {memberLabel(workspace.memberCount)}
                  </span>
                </span>
                {workspace.plan && (
                  <span className="rounded-sm bg-custom-background-80 px-1.5 py-1 text-[11px] font-medium text-custom-text-300">
                    {workspace.plan}
                  </span>
                )}
              </Dropdown.Item>
            ))}
            <Dropdown.Section className="sticky bottom-0 -mx-1 bg-custom-background-90 px-1">
              <Separator className="my-1 border-custom-border-200" />
              <Dropdown.Item
                id="create-workspace"
                textValue="Create workspace"
                href="/create-workspace"
                className="flex h-8 items-center gap-1.5 rounded-md px-2 text-[13px] text-custom-text-100 data-hovered:bg-custom-background-80"
              >
                <CirclePlus aria-hidden="true" className="size-4" />
                <Label>Create workspace</Label>
              </Dropdown.Item>
              <Dropdown.Item
                id="workspace-invites"
                textValue="Workspace invites"
                href="/workspace-invitations"
                className="flex h-8 items-center gap-1.5 rounded-md px-2 text-[13px] text-custom-text-100 data-hovered:bg-custom-background-80"
              >
                <Mail aria-hidden="true" className="size-4" />
                <Label>Workspace invites</Label>
              </Dropdown.Item>
              <Separator className="my-1 border-custom-border-200" />
              <Dropdown.Item
                id="sign-out"
                textValue="Sign out"
                variant="danger"
                onAction={() => setIsOpen(false)}
                className="flex h-8 items-center gap-1.5 rounded-md px-2 text-[13px] text-custom-error-200 data-hovered:bg-custom-background-80"
              >
                <LogOut aria-hidden="true" className="size-4" />
                <Label>Sign out</Label>
              </Dropdown.Item>
            </Dropdown.Section>
          </Dropdown.Menu>
        </Dropdown.Popover>
      </Dropdown>
    </div>
  );
};
