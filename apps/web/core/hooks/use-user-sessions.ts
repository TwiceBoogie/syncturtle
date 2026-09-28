"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import useSWR from "swr";
import { useUser } from "@/hooks/store/user";
import { UserSessionService } from "@/services/user-session.service";
import type { IUserSessionRevocationResponse } from "@syncturtle/types";

export type TUserSessionMutation =
  | { kind: "revoke-session"; sessionId: string }
  | { kind: "revoke-others" }
  | { kind: "revoke-all" };

const SESSION_INVENTORY_KEY = "/api/users/me/sessions";

export const useUserSessions = () => {
  const { isAuthenticated, transitionToSignedOut } = useUser();
  const service = useMemo(() => new UserSessionService(), []);
  const key = isAuthenticated ? SESSION_INVENTORY_KEY : null;
  const {
    data,
    error: loadError,
    isLoading,
    isValidating,
    mutate,
  } = useSWR(key, () => service.listSessions(), {
    shouldRetryOnError: false,
  });
  const mutationPending = useRef(false);
  const wasAuthenticated = useRef(isAuthenticated);
  const [pendingAction, setPendingAction] = useState<TUserSessionMutation | null>(null);
  const [mutationError, setMutationError] = useState<unknown>();

  useEffect(() => {
    if (isAuthenticated) {
      wasAuthenticated.current = true;
      return;
    }
    if (!wasAuthenticated.current) return;
    wasAuthenticated.current = false;
    void mutate(undefined, { revalidate: false });
  }, [isAuthenticated, mutate]);

  const retry = useCallback(async (): Promise<void> => {
    setMutationError(undefined);
    await mutate();
  }, [mutate]);

  const runMutation = useCallback(
    async (action: TUserSessionMutation, operation: () => Promise<IUserSessionRevocationResponse>): Promise<void> => {
      if (mutationPending.current) return;
      mutationPending.current = true;
      setPendingAction(action);
      setMutationError(undefined);
      try {
        const response = await operation();
        if (response.currentSessionRevoked) {
          await mutate(undefined, { revalidate: false });
          transitionToSignedOut();
          return;
        }
        await mutate();
      } catch (error) {
        setMutationError(error);
        throw error;
      } finally {
        mutationPending.current = false;
        setPendingAction(null);
      }
    },
    [mutate, transitionToSignedOut]
  );

  const revokeSession = useCallback(
    (sessionId: string) => runMutation({ kind: "revoke-session", sessionId }, () => service.revokeSession(sessionId)),
    [runMutation, service]
  );
  const revokeOtherSessions = useCallback(
    () => runMutation({ kind: "revoke-others" }, () => service.revokeOtherSessions()),
    [runMutation, service]
  );
  const revokeAllSessions = useCallback(
    () => runMutation({ kind: "revoke-all" }, () => service.revokeAllSessions()),
    [runMutation, service]
  );

  return {
    sessions: data?.sessions,
    error: mutationError ?? loadError,
    isLoading,
    isValidating,
    isMutating: pendingAction !== null,
    pendingAction,
    retry,
    revokeSession,
    revokeOtherSessions,
    revokeAllSessions,
  };
};
