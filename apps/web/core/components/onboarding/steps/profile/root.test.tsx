/** @vitest-environment jsdom */

import type { FormEventHandler, ReactNode } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { IUser } from "@syncturtle/types";
import { EOnboardingSteps } from "@syncturtle/constants";
import { ProfileSetupStep } from "./root";

const mocks = vi.hoisted(() => ({
  user: null as IUser | null,
  updateUser: vi.fn(),
  updateProfile: vi.fn(),
}));
vi.mock("@/hooks/store/user", () => ({
  useUser: () => ({ data: mocks.user, updateCurrentUser: mocks.updateUser }),
  useUserProfile: () => ({ updateUserProfile: mocks.updateProfile }),
}));
vi.mock("@/components/core/modals/user-image-upload-modal", () => ({
  UserImageUploadModal: ({ onFailure }: { onFailure: (message: string) => void }) => (
    <button type="button" onClick={() => onFailure("Upload failed.")}>
      Fail avatar upload
    </button>
  ),
}));
vi.mock("../common", () => ({ CommonOnboardingHeader: ({ title }: { title: string }) => <h1>{title}</h1> }));
vi.mock("./set-password", () => ({ SetPasswordRoot: () => null }));
vi.mock("./consent", () => ({ MarketingConsent: () => null }));
vi.mock("@/services/auth.service", () => ({ AuthService: class {} }));
vi.mock("@heroui/react", () => ({
  toast: Object.assign(vi.fn(), { clear: vi.fn() }),
  Form: ({ children, onSubmit }: { children: ReactNode; onSubmit: FormEventHandler<HTMLFormElement> }) => (
    <form onSubmit={onSubmit}>{children}</form>
  ),
  Button: ({
    children,
    type,
    onPress,
    isDisabled,
  }: {
    children: ReactNode | ((state: { isPending: boolean }) => ReactNode);
    type?: "button" | "submit";
    onPress?: () => void;
    isDisabled?: boolean;
  }) => (
    <button type={type ?? "button"} onClick={onPress} disabled={isDisabled}>
      {typeof children === "function" ? children({ isPending: false }) : children}
    </button>
  ),
  TextField: ({ children, name, isRequired }: { children: ReactNode; name: string; isRequired?: boolean }) => (
    <div data-field={name} data-required={isRequired}>
      {children}
    </div>
  ),
  Input: () => <input />,
  Label: ({ children }: { children: ReactNode }) => <label>{children}</label>,
  FieldError: () => null,
}));

beforeEach(() => {
  vi.resetAllMocks();
  mocks.user = {
    id: "user-1",
    firstName: "Luna",
    lastName: "Snow",
    displayName: "Luna",
    email: "luna@example.test",
    isPasswordAutoset: false,
    avatarAssetId: "asset-1",
    avatarUrl: "/avatars/asset-1",
  } as IUser;
  mocks.updateUser.mockResolvedValue(mocks.user);
  mocks.updateProfile.mockResolvedValue(undefined);
});

describe("optional asset in required Profile step", () => {
  it("continues with the previous owned asset after upload fails", async () => {
    const next = vi.fn().mockResolvedValue(undefined);
    render(<ProfileSetupStep handleStepChange={next} />);
    fireEvent.click(screen.getByRole("button", { name: "Fail avatar upload" }));
    await act(async () => {
      fireEvent.submit(screen.getByRole("button", { name: "Continue" }).closest("form")!);
    });
    expect(mocks.updateUser).toHaveBeenCalledWith(expect.objectContaining({ avatarAssetId: "asset-1" }));
    expect(mocks.updateProfile).toHaveBeenCalledWith({ hasMarketingEmailConsent: true });
    expect(next).toHaveBeenCalledOnce();
    expect(next).toHaveBeenCalledWith(EOnboardingSteps.PROFILE_SETUP);
  });

  it("waits for profile submission before advancing", async () => {
    let resolve!: (user: IUser) => void;
    mocks.updateUser.mockReturnValueOnce(
      new Promise<IUser>((done) => {
        resolve = done;
      })
    );
    const next = vi.fn().mockResolvedValue(undefined);
    render(<ProfileSetupStep handleStepChange={next} />);
    fireEvent.submit(screen.getByRole("button", { name: "Continue" }).closest("form")!);
    expect(next).not.toHaveBeenCalled();
    await act(async () => {
      resolve(mocks.user!);
    });
    expect(next).toHaveBeenCalledOnce();
  });

  it("keeps Profile open when the required user detail write fails", async () => {
    mocks.updateUser.mockRejectedValueOnce(new Error("offline"));
    const next = vi.fn();
    render(<ProfileSetupStep handleStepChange={next} />);
    await act(async () => {
      fireEvent.submit(screen.getByRole("button", { name: "Continue" }).closest("form")!);
    });
    expect(next).not.toHaveBeenCalled();
  });

  it("requires a nonblank last name before submitting to the backend", async () => {
    mocks.user = { ...mocks.user!, lastName: "" };
    const next = vi.fn();
    render(<ProfileSetupStep handleStepChange={next} />);
    expect(screen.getByText("Last name").closest("[data-field]")).toHaveAttribute("data-required", "true");
    expect(screen.getByRole("button", { name: "Continue" })).toBeDisabled();
    await act(async () => {
      fireEvent.submit(screen.getByRole("button", { name: "Continue" }).closest("form")!);
    });
    expect(mocks.updateUser).not.toHaveBeenCalled();
    expect(next).not.toHaveBeenCalled();
  });

  it("uses the valid first name when an existing display name is blank", async () => {
    mocks.user = { ...mocks.user!, displayName: "   " };
    const next = vi.fn();
    render(<ProfileSetupStep handleStepChange={next} />);
    await act(async () => {
      fireEvent.submit(screen.getByRole("button", { name: "Continue" }).closest("form")!);
    });
    expect(mocks.updateUser).toHaveBeenCalledWith(expect.objectContaining({ displayName: "Luna" }));
    expect(next).toHaveBeenCalledOnce();
  });
});
