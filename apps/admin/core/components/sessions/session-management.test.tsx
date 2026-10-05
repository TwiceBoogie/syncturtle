/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { IUserSession } from "@syncturtle/types";
import { SessionManagementPanel } from ".";

type TPendingAction =
  { kind: "revoke-session"; sessionId: string } | { kind: "revoke-others" } | { kind: "revoke-all" };

const actions = vi.hoisted(() => ({
  retry: vi.fn<() => Promise<void>>(),
  revokeSession: vi.fn<(sessionId: string) => Promise<void>>(),
  revokeOtherSessions: vi.fn<() => Promise<void>>(),
  revokeAllSessions: vi.fn<() => Promise<void>>(),
}));

const hook = vi.hoisted(() => ({
  sessions: undefined as IUserSession[] | undefined,
  error: undefined as unknown,
  isLoading: true,
  isValidating: false,
  isMutating: false,
  pendingAction: null as TPendingAction | null,
}));

vi.mock("@/hooks/use-user-sessions", () => ({
  useUserSessions: () => ({ ...hook, ...actions }),
}));

vi.mock("@heroui/react", () => {
  const Button = ({
    children,
    isDisabled,
    onPress,
  }: {
    children?: ReactNode;
    isDisabled?: boolean;
    onPress?: () => void;
  }) => (
    <button type="button" disabled={isDisabled} onClick={onPress}>
      {children}
    </button>
  );
  const Container = ({ children }: { children?: ReactNode }) => <div>{children}</div>;
  const Backdrop = ({ children, isOpen }: { children?: ReactNode; isOpen?: boolean }) =>
    isOpen ? <div>{children}</div> : null;
  const Dialog = ({
    children,
    ...props
  }: {
    children?: ReactNode;
    "aria-labelledby"?: string;
    "aria-describedby"?: string;
  }) => (
    <div role="dialog" {...props}>
      {children}
    </div>
  );
  return {
    Button,
    Chip: Container,
    ScrollShadow: Container,
    Spinner: ({ "aria-label": ariaLabel }: { "aria-label"?: string }) => <span role="status" aria-label={ariaLabel} />,
    Modal: {
      Backdrop,
      Container,
      Dialog,
      Header: Container,
      Heading: ({ children, id }: { children?: ReactNode; id?: string }) => <h2 id={id}>{children}</h2>,
      Body: Container,
      Footer: Container,
      CloseTrigger: () => <button aria-label="Close" type="button" />,
    },
  };
});

const currentSession: IUserSession = {
  sessionId: "00000000-0000-4000-8000-000000000001",
  current: true,
  administrator: true,
  createdAt: "2026-09-01T00:00:00Z",
  lastUsedAt: "2026-09-22T12:00:00Z",
  idleExpiresAt: "2026-09-29T12:00:00Z",
  absoluteExpiresAt: "2026-10-01T00:00:00Z",
};

const otherSession: IUserSession = {
  sessionId: "00000000-0000-4000-8000-000000000002",
  current: false,
  administrator: false,
  createdAt: "",
  lastUsedAt: "not-a-date",
  idleExpiresAt: "invalid",
  absoluteExpiresAt: "2026-10-02T00:00:00Z",
};

function deferred() {
  let resolve!: () => void;
  const promise = new Promise<void>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}

beforeEach(() => {
  vi.clearAllMocks();
  hook.sessions = undefined;
  hook.error = undefined;
  hook.isLoading = true;
  hook.isValidating = false;
  hook.isMutating = false;
  hook.pendingAction = null;
  actions.retry.mockReset().mockResolvedValue(undefined);
  actions.revokeSession.mockReset().mockResolvedValue(undefined);
  actions.revokeOtherSessions.mockReset().mockResolvedValue(undefined);
  actions.revokeAllSessions.mockReset().mockResolvedValue(undefined);
});

describe("administrator session management", () => {
  it("presents loading, empty, validation, and mutation states", () => {
    const view = render(<SessionManagementPanel />);
    expect(screen.getByText("Loading active sessions...")).toBeInTheDocument();

    hook.sessions = [];
    hook.isLoading = false;
    view.rerender(<SessionManagementPanel />);
    expect(screen.getByRole("heading", { name: "No active sessions" })).toBeInTheDocument();
    expect(screen.getByText("0 active sessions")).toBeInTheDocument();

    hook.sessions = [currentSession];
    hook.isValidating = true;
    view.rerender(<SessionManagementPanel />);
    expect(screen.getByText("Refreshing active sessions...")).toBeInTheDocument();

    hook.isValidating = false;
    hook.isMutating = true;
    hook.pendingAction = { kind: "revoke-all" };
    view.rerender(<SessionManagementPanel />);
    expect(screen.getByText("Updating active sessions...")).toBeInTheDocument();
  });

  it("shows a safe load error and permits explicit retry", () => {
    hook.error = new Error("private upstream payload");
    render(<SessionManagementPanel />);

    expect(screen.getByRole("alert")).toHaveTextContent("Unable to load active sessions. Please try again.");
    expect(screen.queryByText(/private upstream payload/)).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(actions.retry).toHaveBeenCalledTimes(1);
  });

  it("retains inventory on an operational error and exposes retry", () => {
    hook.sessions = [currentSession, otherSession];
    hook.error = new Error("private upstream payload");
    hook.isLoading = false;
    render(<SessionManagementPanel />);

    expect(screen.getAllByRole("article")).toHaveLength(2);
    expect(screen.getByRole("alert")).toHaveTextContent("The displayed list was retained.");
    expect(screen.queryByText(/private upstream payload/)).toBeNull();
    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(actions.retry).toHaveBeenCalledTimes(1);
  });

  it("uses only trusted flags, presents every timestamp, and safely falls back", () => {
    hook.sessions = [currentSession, otherSession];
    hook.isLoading = false;
    render(<SessionManagementPanel />);

    const current = screen.getByRole("article", { name: "Current session" });
    expect(within(current).getByText("Current session")).toBeInTheDocument();
    expect(within(current).getByText("Administrator session")).toBeInTheDocument();

    const other = screen.getByRole("article", { name: "Active session" });
    expect(within(other).queryByText("Current session")).toBeNull();
    expect(within(other).queryByText("Administrator session")).toBeNull();
    expect(screen.getAllByText("Created")).toHaveLength(2);
    expect(screen.getAllByText("Last used")).toHaveLength(2);
    expect(screen.getAllByText("Idle expiration")).toHaveLength(2);
    expect(screen.getAllByText("Absolute expiration")).toHaveLength(2);
    expect(screen.getAllByText("Unavailable")).toHaveLength(3);
    expect(screen.queryByText(/browser|device|operating system|location|ip address/i)).toBeNull();
  });

  it("requires confirmation and suppresses duplicate revoke-one action", async () => {
    const pending = deferred();
    actions.revokeSession.mockReturnValueOnce(pending.promise);
    hook.sessions = [currentSession, otherSession];
    hook.isLoading = false;
    render(<SessionManagementPanel />);

    fireEvent.click(within(screen.getByRole("article", { name: "Active session" })).getByRole("button"));
    expect(actions.revokeSession).not.toHaveBeenCalled();
    const dialog = screen.getByRole("dialog", { name: "Revoke this session?" });
    expect(dialog).toHaveTextContent("only the selected session");
    const confirm = within(dialog).getByRole("button", { name: "Revoke session" });
    fireEvent.click(confirm);
    expect(actions.revokeSession).toHaveBeenCalledWith(otherSession.sessionId);
    expect(confirm).toBeDisabled();
    expect(within(screen.getByRole("article", { name: "Current session" })).getByRole("button")).toBeDisabled();
    expect(screen.getByRole("button", { name: "Revoke other sessions" })).toBeDisabled();
    fireEvent.click(confirm);
    expect(actions.revokeSession).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("article", { name: "Active session" })).toBeInTheDocument();

    await act(async () => pending.resolve());
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("cancels without revoking and leaves the inventory intact", () => {
    hook.sessions = [currentSession, otherSession];
    hook.isLoading = false;
    render(<SessionManagementPanel />);

    fireEvent.click(screen.getByRole("button", { name: "Revoke all sessions" }));
    const dialog = screen.getByRole("dialog", { name: "Revoke all sessions?" });
    fireEvent.click(within(dialog).getByRole("button", { name: "Cancel" }));

    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.getAllByRole("article")).toHaveLength(2);
    expect(actions.revokeSession).not.toHaveBeenCalled();
    expect(actions.revokeOtherSessions).not.toHaveBeenCalled();
    expect(actions.revokeAllSessions).not.toHaveBeenCalled();
  });

  it("keeps a failed revoke visible and retries only after a deliberate action", async () => {
    const failure = new Error("private upstream payload");
    actions.revokeSession.mockRejectedValueOnce(failure);
    hook.sessions = [currentSession, otherSession];
    hook.isLoading = false;
    const view = render(<SessionManagementPanel />);

    fireEvent.click(within(screen.getByRole("article", { name: "Active session" })).getByRole("button"));
    await act(async () => {
      fireEvent.click(
        within(screen.getByRole("dialog", { name: "Revoke this session?" })).getByRole("button", {
          name: "Revoke session",
        })
      );
    });

    expect(actions.revokeSession).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("dialog")).toBeNull();
    expect(screen.getAllByRole("article")).toHaveLength(2);

    hook.error = failure;
    view.rerender(<SessionManagementPanel />);
    expect(screen.getByRole("alert")).toHaveTextContent("The displayed list was retained.");
    expect(screen.queryByText(/private upstream payload/)).toBeNull();
    expect(actions.revokeSession).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByRole("button", { name: "Retry" }));
    expect(actions.retry).toHaveBeenCalledTimes(1);
    fireEvent.click(within(screen.getByRole("article", { name: "Active session" })).getByRole("button"));
    await act(async () => {
      fireEvent.click(
        within(screen.getByRole("dialog", { name: "Revoke this session?" })).getByRole("button", {
          name: "Revoke session",
        })
      );
    });
    expect(actions.revokeSession).toHaveBeenCalledTimes(2);
  });

  it("uses current-session wording without owning the terminal transition", async () => {
    hook.sessions = [currentSession];
    hook.isLoading = false;
    render(<SessionManagementPanel />);

    fireEvent.click(within(screen.getByRole("article", { name: "Current session" })).getByRole("button"));
    const dialog = screen.getByRole("dialog", { name: "Revoke current session?" });
    expect(dialog).toHaveTextContent("only after the server confirms current-session revocation");
    await act(async () => {
      fireEvent.click(within(dialog).getByRole("button", { name: "Revoke current session" }));
    });
    expect(actions.revokeSession).toHaveBeenCalledWith(currentSession.sessionId);
    expect(actions.revokeOtherSessions).not.toHaveBeenCalled();
    expect(actions.revokeAllSessions).not.toHaveBeenCalled();
  });

  it("confirms other-session scope and suppresses a duplicate action", async () => {
    const pending = deferred();
    actions.revokeOtherSessions.mockReturnValueOnce(pending.promise);
    hook.sessions = [currentSession, otherSession];
    hook.isLoading = false;
    render(<SessionManagementPanel />);

    fireEvent.click(screen.getByRole("button", { name: "Revoke other sessions" }));
    const dialog = screen.getByRole("dialog", { name: "Revoke other sessions?" });
    expect(dialog).toHaveTextContent("every other active session");
    const confirm = within(dialog).getByRole("button", { name: "Revoke other sessions" });
    fireEvent.click(confirm);
    fireEvent.click(confirm);
    expect(actions.revokeOtherSessions).toHaveBeenCalledTimes(1);
    expect(screen.getAllByRole("article")).toHaveLength(2);

    await act(async () => pending.resolve());
  });

  it("distinguishes revoke-all scope and delegates exactly once to the hook", async () => {
    const pending = deferred();
    actions.revokeAllSessions.mockReturnValueOnce(pending.promise);
    hook.sessions = [currentSession, otherSession];
    hook.isLoading = false;
    render(<SessionManagementPanel />);

    fireEvent.click(screen.getByRole("button", { name: "Revoke all sessions" }));
    const dialog = screen.getByRole("dialog", { name: "Revoke all sessions?" });
    expect(dialog).toHaveTextContent("including the current session");
    expect(dialog).toHaveTextContent("only after the server confirms current-session revocation");
    const confirm = within(dialog).getByRole("button", { name: "Revoke all sessions" });
    fireEvent.click(confirm);
    fireEvent.click(confirm);
    expect(actions.revokeAllSessions).toHaveBeenCalledTimes(1);
    expect(actions.revokeSession).not.toHaveBeenCalled();
    expect(actions.revokeOtherSessions).not.toHaveBeenCalled();

    await act(async () => pending.resolve());
  });
});
