/** @vitest-environment jsdom */

import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import { ProfileSettingContentHeader } from "@/components/profile/profile-setting-content-header";

describe("ProfileSettingContentHeader", () => {
  it("shows the title and optional description", () => {
    render(<ProfileSettingContentHeader title="Security" description="Manage your account" />);

    expect(screen.getByText("Security")).toBeInTheDocument();
    expect(screen.getByText("Manage your account")).toBeInTheDocument();
  });

  it("omits the description when none is provided", () => {
    render(<ProfileSettingContentHeader title="Security" />);

    expect(screen.getByText("Security")).toBeInTheDocument();
    expect(screen.queryByText("Manage your account")).not.toBeInTheDocument();
  });
});
