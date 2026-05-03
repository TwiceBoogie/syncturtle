"use client";

import { useState } from "react";
// hooks
import { useInstance } from "@/hooks/store/use-instance";
import { useWorkspace } from "@/hooks/store/use-workspace";
import useSWR from "swr";
import { Button, Spinner, Switch, toast } from "@heroui/react";
import { TInstanceConfigurationKeys } from "@syncturtle/types";
import Link from "next/link";
import { WorkspaceListItem } from "@/components/workspace/list-item";

export default function WorkspaceMangagementPage() {
  // states
  const [isSubmitting, setIsSubmitting] = useState(false);
  // store hooks
  const { formattedConfig, fetchInstanceConfigurations, updateInstanceConfigurations } = useInstance();
  const {
    workspaceIds,
    loader: workspaceLoader,
    paginationInfo,
    fetchWorkspaces,
    fetchNextWorkspaces,
  } = useWorkspace();
  // derived values
  const disableWorkspaceCreation = formattedConfig?.DISABLE_WORKSPACE_CREATION ?? "";
  const hasNextPage = paginationInfo?.nextPageResults && paginationInfo.nextCursor != undefined;

  useSWR("INSTANCE_CONFIGURATIONS", () => fetchInstanceConfigurations());
  useSWR("INSTANCE_WORKSPACES", () => fetchWorkspaces());

  const updateConfig = async (key: TInstanceConfigurationKeys, value: string) => {
    if (isSubmitting) return;
    setIsSubmitting(true);

    const payload: Partial<Record<TInstanceConfigurationKeys, string>> = {
      [key]: value,
    };

    try {
      toast.promise(updateInstanceConfigurations(payload), {
        loading: "Saving configurations...",
        success: () => "Configuration saved successfully",
        error: () => "Failed to save configuration",
      });
    } finally {
      setIsSubmitting(false);
    }
  };
  return (
    <div className="relative container mx-auto w-full h-full p-4 py-4 space-y-6 flex flex-col">
      <div className="flex items-center justify-between gap-4 border-b border-custom-border-100 mx-4 py-4 space-y-1 shrink-0">
        <div className="flex flex-col gap-1">
          <div className="text-xl font-medium">Workspaces on this instance</div>
          <div className="text-sm font-normal">See all workspaces and control who can create them.</div>
        </div>
      </div>
      <div className="grow overflow-hidden overflow-y-scroll px-4">
        <div className="space-y-3">
          {formattedConfig ? (
            <div className="w-full flex items-center gap-14 rounded">
              <div className="flex grow items-center gap-4">
                <div className="grow">
                  <div className="text-lg font-medium pb-1">Prevent anyone else from creating a workspace.</div>
                  <div className="font-normal leading-5 text-custom-text-300 text-xs">
                    Toggling this on will let only you create workspaces. You will have to invite users to new
                    workspaces.
                  </div>
                </div>
              </div>
              <Switch
                aria-label="Disable workspace creation"
                size="sm"
                name="disableWorkspaceCreation"
                isSelected={Boolean(parseInt(disableWorkspaceCreation))}
                onChange={(isSelected: boolean) => {
                  updateConfig("DISABLE_WORKSPACE_CREATION", isSelected ? "1" : "0");
                }}
                isDisabled={isSubmitting}
              >
                <Switch.Control>
                  <Switch.Thumb>
                    <Switch.Icon />
                  </Switch.Thumb>
                </Switch.Control>
              </Switch>
            </div>
          ) : (
            <Spinner />
          )}
          {workspaceLoader !== "init-loader" ? (
            <>
              <div className="pt-6 flex items-center justify-between gap-2">
                <div className="flex flex-col items-start gap-x-2">
                  <div className="">
                    All workspaces on this instance{" "}
                    <span className="text-custom-text-300">• {workspaceIds.length}</span>
                    {workspaceLoader && ["mutation", "pagination"].includes(workspaceLoader) && <Spinner />}
                  </div>
                  <div className="font-normal leading-5 text-custom-text-300 text-xs">
                    You can&apos;t yet delete workspaces and you can only go to the workspace if you are an Admin or a
                    Member.
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <Link href={`/workspace/create`}>
                    <Button>Create workspace</Button>
                  </Link>
                </div>
              </div>
              <div className="flex flex-col gap-4 py-2">
                {workspaceIds.map((workspaceId) => (
                  <WorkspaceListItem key={workspaceId} workspaceId={workspaceId} />
                ))}
              </div>
              {hasNextPage && (
                <div className="">
                  <Button isDisabled={workspaceLoader === "pagination"} onPress={() => fetchNextWorkspaces()}>
                    Load more
                  </Button>
                </div>
              )}
            </>
          ) : (
            <Spinner />
          )}
        </div>
      </div>
    </div>
  );
}
