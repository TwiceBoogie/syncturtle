"use client";

import { useRef, useState } from "react";
// heroui
import { Button, ScrollShadow, Spinner } from "@heroui/react";
// hooks
import { useUserSessions } from "@/hooks/use-user-sessions";
// components
import { SessionCard } from "./session-card";
import { RevokeSessionsModal } from "./revoke-sessions-modal";
import type { IUserSession } from "@syncturtle/types";

type TConfirmation =
  | { kind: "revoke-session"; session: IUserSession }
  | { kind: "revoke-others" }
  | { kind: "revoke-all" };

const getConfirmationContent = (confirmation: TConfirmation | null) => {
  if (!confirmation) return { title: "Revoke sessions", description: "", confirmLabel: "" };
  if (confirmation.kind === "revoke-others") {
    return {
      title: "Revoke other sessions?",
      description:
        "This revokes every other active session. This session remains active unless the server reports otherwise.",
      confirmLabel: "Revoke other sessions",
    };
  }
  if (confirmation.kind === "revoke-all") {
    return {
      title: "Revoke all sessions?",
      description:
        "This revokes every active session, including the current session. You will be signed out only after the server confirms current-session revocation.",
      confirmLabel: "Revoke all sessions",
    };
  }
  if (confirmation.session.current) {
    return {
      title: "Revoke current session?",
      description:
        "This revokes the current session. You will be signed out only after the server confirms current-session revocation.",
      confirmLabel: "Revoke current session",
    };
  }
  return {
    title: "Revoke this session?",
    description: "This revokes only the selected session after the server confirms the operation.",
    confirmLabel: "Revoke session",
  };
};

export const SessionManagementPanel = () => {
  // hooks
  const {
    sessions,
    error,
    isLoading,
    isValidating,
    isMutating,
    pendingAction,
    retry,
    revokeSession,
    revokeOtherSessions,
    revokeAllSessions,
  } = useUserSessions();
  const confirmationPending = useRef(false);
  // state
  const [confirmation, setConfirmation] = useState<TConfirmation | null>(null);
  const [isConfirming, setIsConfirming] = useState(false);

  const handleRetry = () => {
    void retry().catch(() => undefined);
  };

  const closeConfirmation = () => {
    if (!confirmationPending.current) setConfirmation(null);
  };

  const openConfirmation = (action: TConfirmation) => {
    if (confirmationPending.current || isMutating) return;
    setConfirmation(action);
  };

  const confirmRevocation = async (): Promise<void> => {
    if (!confirmation || confirmationPending.current) return;
    confirmationPending.current = true;
    setIsConfirming(true);
    try {
      if (confirmation.kind === "revoke-session") {
        await revokeSession(confirmation.session.sessionId);
      } else if (confirmation.kind === "revoke-others") {
        await revokeOtherSessions();
      } else {
        await revokeAllSessions();
      }
    } catch {
      // The hook preserves the last authoritative inventory and exposes a safe retry state.
    } finally {
      confirmationPending.current = false;
      setIsConfirming(false);
      setConfirmation(null);
    }
  };

  if (sessions === undefined) {
    return (
      <div className="flex min-h-48 flex-1 items-center justify-center px-4">
        {error ? (
          <div className="max-w-md space-y-3 text-center" role="alert">
            <p className="text-sm text-danger">Unable to load active sessions. Please try again.</p>
            <Button variant="outline" onPress={handleRetry}>
              Retry
            </Button>
          </div>
        ) : (
          <div className="flex items-center gap-2 text-sm text-custom-text-300" role="status">
            <Spinner aria-label="Loading active sessions" size="sm" />
            {isLoading ? "Loading active sessions..." : "Waiting for active sessions..."}
          </div>
        )}
      </div>
    );
  }

  const modalContent = getConfirmationContent(confirmation);
  const hasOtherSessions = sessions?.some((session) => !session.current);

  return (
    <>
      <ScrollShadow orientation="vertical" size={40} className="min-h-0 flex-1 px-4 pb-4">
        <div className="space-y-4">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <div aria-live="polite" className="min-h-5 text-sm text-custom-text-300">
              {isMutating
                ? "Updating active sessions..."
                : isValidating
                  ? "Refreshing active sessions..."
                  : `${sessions?.length} active ${sessions?.length === 1 ? "session" : "sessions"}`}
            </div>
            <div className="flex flex-col gap-2 sm:flex-row">
              <Button
                variant="outline"
                isDisabled={!hasOtherSessions || isMutating || isConfirming}
                onPress={() => openConfirmation({ kind: "revoke-others" })}
              >
                Revoke other sessions
              </Button>
              <Button
                variant="danger"
                isDisabled={sessions?.length === 0 || isMutating || isConfirming}
                onPress={() => openConfirmation({ kind: "revoke-all" })}
              >
                Revoke all sessions
              </Button>
            </div>
          </div>

          {error && (
            <div
              className="flex flex-col gap-3 rounded-lg border border-red-400 bg-red-500/5 p-4 sm:flex-row sm:items-center sm:justify-between"
              role="alert"
            >
              <p className="text-sm text-danger">
                Unable to load or update active sessions. The displayed list was retained.
              </p>
              <Button variant="outline" isDisabled={isMutating} onPress={handleRetry}>
                Retry
              </Button>
            </div>
          )}

          {sessions?.length === 0 ? (
            <div className="rounded-lg border border-dashed border-custom-border-100 p-8 text-center">
              <h2 className="text-base font-medium text-custom-text-100">No active sessions</h2>
              <p className="mt-1 text-sm text-custom-text-300">No session inventory is currently available.</p>
            </div>
          ) : (
            <div className="space-y-2">
              {sessions?.map((session) => (
                <SessionCard
                  key={session.sessionId}
                  session={session}
                  isMutationPending={
                    isMutating &&
                    pendingAction?.kind === "revoke-session" &&
                    pendingAction.sessionId === session.sessionId
                  }
                  isActionDisabled={isMutating || isConfirming}
                  onRequestRevoke={(selectedSession) =>
                    openConfirmation({ kind: "revoke-session", session: selectedSession })
                  }
                />
              ))}
            </div>
          )}
        </div>
      </ScrollShadow>

      <RevokeSessionsModal
        isOpen={confirmation !== null}
        title={modalContent.title}
        description={modalContent.description}
        confirmLabel={modalContent.confirmLabel}
        isPending={isConfirming}
        onClose={closeConfirmation}
        onConfirm={() => void confirmRevocation()}
      />
    </>
  );
};
