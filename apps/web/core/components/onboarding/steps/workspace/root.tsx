"use client";

import { useEffect, useRef, useState } from "react";
// heroui
import { Button, Spinner } from "@heroui/react";
// components
import { WorkspaceCreateStep } from "./create";
// hooks
import { useUser, useUserProfile, useUserSettings } from "@/hooks/store/user";
import { useWorkspace } from "@/hooks/store/use-workspace";
import { useInstance } from "@/hooks/store/use-instance";
// types
import type { IWorkspace } from "@syncturtle/types";

type Props = { onComplete: (workspace: IWorkspace) => Promise<void>; onBusyChange?: (busy: boolean) => void };

export const WorkspaceSetupStep = ({ onComplete, onBusyChange }: Props) => {
  // hooks
  const { data: user } = useUser();
  const { data: profile, updateUserProfile } = useUserProfile();
  const { fetchCurrentUserSettings } = useUserSettings();
  const { workspaces, loader, hasLoaded, error, fetchWorkspaces } = useWorkspace();
  const { config, error: instanceError, fetchInstanceInfo } = useInstance();
  // states
  const [selectedId, setSelectedId] = useState(profile.lastWorkspaceId ?? "");
  const [isSelecting, setIsSelecting] = useState(false);
  const selecting = useRef(false);
  const [isPosting, setIsPosting] = useState(false);
  const [selectionError, setSelectionError] = useState<string>();
  const [isCreating, setIsCreating] = useState(false);

  useEffect(() => {
    onBusyChange?.(isSelecting || isPosting);
  }, [isSelecting, isPosting, onBusyChange]);

  useEffect(() => () => onBusyChange?.(false), [onBusyChange]);
  // derived values
  const list = Object.values(workspaces);
  const selected = list.find((space) => space.id === selectedId) ?? list[0];

  const selectSpace = async (space: IWorkspace) => {
    if (selecting.current) return;
    selecting.current = true;
    setIsSelecting(true);
    setSelectionError(undefined);
    try {
      await updateUserProfile({ lastWorkspaceId: space.id });
      await fetchCurrentUserSettings(true);
      await onComplete(space);
    } catch (failure) {
      setSelectionError("Could not save your space selection. Please try again.");
      throw failure;
    } finally {
      selecting.current = false;
      setIsSelecting(false);
    }
  };

  if (loader || (!hasLoaded && !error)) return <Spinner />;

  if (error && list.length === 0)
    return (
      <div role="alert">
        <p>Unable to load spaces.</p>
        <Button onPress={() => void fetchWorkspaces().catch(() => {})}>Retry spaces</Button>
      </div>
    );

  if (!config && list.length === 0)
    return (
      <div role={instanceError ? "alert" : "status"}>
        <p>{instanceError ? "Unable to check space creation policy." : "Checking space creation policy..."}</p>
        <Button onPress={() => void fetchInstanceInfo().catch(() => {})}>Retry policy</Button>
      </div>
    );

  return (
    <div className="flex flex-col gap-6">
      {selectionError && <p role="alert">{selectionError}</p>}
      {list.length > 0 && (!isCreating || config?.isWorkspaceCreationDisabled !== false) && (
        <>
          <h1 className="text-2xl">Choose a space</h1>
          <label htmlFor="existing-space">Your spaces</label>
          <select
            id="existing-space"
            value={selected?.id ?? ""}
            disabled={isSelecting || isPosting}
            onChange={(event) => setSelectedId(event.target.value)}
          >
            {list.map((space) => (
              <option key={space.id} value={space.id}>
                {space.name}
              </option>
            ))}
          </select>
          <Button
            isDisabled={isSelecting || isPosting || !selected}
            isPending={isSelecting}
            onPress={() => {
              if (selected) void selectSpace(selected).catch(() => {});
            }}
          >
            Use this space
          </Button>
          {config?.isWorkspaceCreationDisabled === false && (
            <Button variant="secondary" isDisabled={isSelecting} onPress={() => setIsCreating(true)}>
              Create another space
            </Button>
          )}
        </>
      )}
      {config?.isWorkspaceCreationDisabled && list.length === 0 && (
        <div role="status">
          <h1>Space creation is unavailable</h1>
          <p>
            This installation does not allow workspace creation. Ask your instance administrator to arrange an active
            membership or enable creation.
          </p>
          <p>
            Pending invitation acceptance is not available in this version. An invitation alone does not grant access.
          </p>
          <Button onPress={() => void Promise.all([fetchWorkspaces(), fetchInstanceInfo()]).catch(() => {})}>
            Retry availability
          </Button>
        </div>
      )}
      {config?.isWorkspaceCreationDisabled === false && (list.length === 0 || isCreating) && (
        <>
          <WorkspaceCreateStep
            user={user}
            onComplete={selectSpace}
            onBusyChange={(busy) => {
              setIsPosting(busy);
              if (busy) setIsCreating(true);
            }}
          />
          {list.length > 0 && (
            <Button variant="secondary" isDisabled={isSelecting || isPosting} onPress={() => setIsCreating(false)}>
              Choose an existing space
            </Button>
          )}
        </>
      )}
      <p className="text-sm">
        A space is complete with just you. You can share it later. Pending invitation acceptance is coming in a later
        version.
      </p>
    </div>
  );
};
