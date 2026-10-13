/** @vitest-environment jsdom */

import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { RevokeSessionsModal } from "./revoke-sessions-modal";

describe("session revocation dialog accessibility", () => {
  it("does not create an unnamed dialog while closed", () => {
    const warning = vi.spyOn(console, "warn").mockImplementation(() => undefined);

    try {
      render(
        <RevokeSessionsModal
          isOpen={false}
          title=""
          description=""
          confirmLabel=""
          isPending={false}
          onClose={() => undefined}
          onConfirm={() => undefined}
        />
      );

      expect(screen.queryByRole("dialog")).toBeNull();
      expect(warning).not.toHaveBeenCalledWith(
        'If a Dialog does not contain a <Heading slot="title">, it must have an aria-label or aria-labelledby attribute for accessibility.'
      );
    } finally {
      warning.mockRestore();
    }
  });

  it.each([
    ["Revoke this session?", "This revokes only the selected session."],
    ["Revoke current session?", "This revokes the current session."],
    ["Revoke other sessions?", "This revokes every other active session."],
    ["Revoke all sessions?", "This revokes every active session."],
  ])("names the real HeroUI dialog for %s", (title, description) => {
    const warning = vi.spyOn(console, "warn").mockImplementation(() => undefined);

    try {
      render(
        <RevokeSessionsModal
          isOpen
          title={title}
          description={description}
          confirmLabel="Revoke other sessions"
          isPending={false}
          onClose={() => undefined}
          onConfirm={() => undefined}
        />
      );

      const dialog = screen.getByRole("dialog", { name: title });
      const heading = screen.getByRole("heading", { name: title });
      expect(dialog).toHaveAttribute("aria-labelledby", heading.id);
      expect(dialog).toHaveAttribute("aria-describedby", screen.getByText(description).id);
      expect(warning).not.toHaveBeenCalledWith(
        'If a Dialog does not contain a <Heading slot="title">, it must have an aria-label or aria-labelledby attribute for accessibility.'
      );
    } finally {
      warning.mockRestore();
    }
  });

  it("closes by keyboard when idle and keeps a pending action disabled", async () => {
    const onClose = vi.fn<() => void>();
    const user = userEvent.setup();
    const view = render(
      <RevokeSessionsModal
        isOpen
        title="Revoke all sessions?"
        description="This revokes every active session."
        confirmLabel="Revoke all sessions"
        isPending={false}
        onClose={onClose}
        onConfirm={() => undefined}
      />
    );

    await user.keyboard("{Escape}");
    expect(onClose).toHaveBeenCalledTimes(1);

    view.rerender(
      <RevokeSessionsModal
        isOpen
        title="Revoke all sessions?"
        description="This revokes every active session."
        confirmLabel="Revoke all sessions"
        isPending
        onClose={onClose}
        onConfirm={() => undefined}
      />
    );
    expect(screen.getByRole("button", { name: "Cancel" })).toBeDisabled();
    expect(screen.getByRole("button", { name: /Revoking/ })).toBeDisabled();
    await user.keyboard("{Escape}");
    expect(onClose).toHaveBeenCalledTimes(1);
  });
});
