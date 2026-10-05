/** @vitest-environment jsdom */

import type { ReactNode } from "react";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { IUser, IUserSettings, IWorkspace, TUserProfile } from "@syncturtle/types";
import type { CoreRootStore } from "../../store/root.store";
import type { RootStore } from "../../../ce/store/root.store";
import { UserStore } from "../../store/user";
import { WorkspaceStore } from "../../store/workspace";
import { StoreContext } from "../store-context";
import { AuthenticationWrapper } from "./authentication-wrapper";
import { OnboardingRoot } from "../../components/onboarding/root";
import { EPageType } from "../../../helpers/authentication.helper";

const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  replace: vi.fn(),
  currentUser: vi.fn(),
  profile: vi.fn(),
  settings: vi.fn(),
  updateProfile: vi.fn(),
  onboard: vi.fn(),
  workspaces: vi.fn(),
  create: vi.fn(),
}));
vi.mock("next/navigation", () => ({
  usePathname: () => "/onboarding/",
  useSearchParams: () => new URLSearchParams(),
  useRouter: () => ({ push: mocks.push, replace: mocks.replace }),
}));
vi.mock("swr", () => ({ default: () => ({ isLoading: false }) }));
vi.mock("@heroui/react", () => ({
  Spinner: () => <div role="status">Loading</div>,
  Button: ({ children, onPress, isDisabled }: { children: ReactNode; onPress: () => void; isDisabled?: boolean }) => (
    <button type="button" onClick={onPress} disabled={isDisabled}>
      {children}
    </button>
  ),
}));
vi.mock("../../components/onboarding/header", () => ({ OnboardingHeader: () => null }));
vi.mock("../../components/onboarding/steps/common", () => ({
  CommonOnboardingHeader: ({ title }: { title: string }) => <h1>{title}</h1>,
}));
vi.mock("../../components/onboarding/steps/workspace/create", () => ({ WorkspaceCreateStep: () => null }));
vi.mock("@/hooks/store/use-instance", () => ({
  useInstance: () => ({ config: { isWorkspaceCreationDisabled: true }, fetchInstanceInfo: vi.fn() }),
}));
vi.mock("@/services/user.service", () => ({
  UserService: class {
    currentUser = mocks.currentUser;
    getCurrentUserProfile = mocks.profile;
    currentUserSettings = mocks.settings;
    updateCurrentUserProfile = mocks.updateProfile;
    updateUserOnboard = mocks.onboard;
  },
}));
vi.mock("@/services/workspace.service", () => ({
  WorkspaceService: class {
    userWorkspaces = mocks.workspaces;
    createWorkspace = mocks.create;
  },
}));
vi.mock("@/services/auth.service", () => ({ AuthService: class {} }));
vi.mock("@syncturtle/utils", async () => ({
  ...(await import("../../../../../packages/utils/src/external-store")),
}));

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>((done) => {
    resolve = done;
  });
  return { promise, resolve };
}

const workspace = { id: "workspace-1", slug: "turtles", name: "Turtles" } as IWorkspace;
let store: CoreRootStore;
let profile: TUserProfile;

beforeEach(() => {
  vi.resetAllMocks();
  store = { router: {} } as CoreRootStore;
  store.user = new UserStore(store);
  store.workspace = new WorkspaceStore(store);
  profile = { ...store.user.userProfile.data, id: "profile-1" };
  mocks.currentUser.mockResolvedValue({ id: "user-1" } as IUser);
  mocks.profile.mockImplementation(async () => profile);
  mocks.settings.mockResolvedValue(store.user.userSettings.data);
  mocks.workspaces.mockResolvedValue([]);
  mocks.create.mockResolvedValue(workspace);
  mocks.updateProfile.mockImplementation(async (data: Partial<TUserProfile>) => ({
    ...profile,
    ...data,
    onboardingStep: { ...profile.onboardingStep, ...data.onboardingStep },
  }));
  mocks.onboard.mockResolvedValue(undefined);
});

function mount(pageType = EPageType.ONBOARDING) {
  return render(
    <StoreContext.Provider value={store as RootStore}>
      <AuthenticationWrapper pageType={pageType}>Page content</AuthenticationWrapper>
    </StoreContext.Provider>
  );
}

describe("workspace routing lifecycle", () => {
  it("keeps Space and purpose mounted through refreshes, then navigates to the selected slug", async () => {
    Object.defineProperty(HTMLElement.prototype, "scrollTo", { configurable: true, value: vi.fn() });
    const personal = { ...workspace, id: "personal-id", slug: "personal-49bb21d9", name: "Personal" };
    profile = { ...profile, onboardingStep: { ...profile.onboardingStep, profileComplete: true } };
    mocks.workspaces.mockResolvedValue([workspace, personal]);
    await store.user.fetchCurrentUser();
    const selectionRefresh = deferred<IUserSettings>();
    mocks.settings.mockReturnValueOnce(selectionRefresh.promise);

    render(
      <StoreContext.Provider value={store as RootStore}>
        <AuthenticationWrapper pageType={EPageType.ONBOARDING}>
          <OnboardingRoot />
        </AuthenticationWrapper>
      </StoreContext.Provider>
    );
    fireEvent.change(screen.getByLabelText("Your spaces"), { target: { value: personal.id } });
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Use this space" }));
    });
    expect(store.user.userProfile.data.lastWorkspaceId).toBe(personal.id);
    expect(screen.getByRole("button", { name: "Use this space" })).toBeInTheDocument();
    expect(mocks.onboard).not.toHaveBeenCalled();
    expect(mocks.replace).not.toHaveBeenCalled();

    await act(async () => {
      selectionRefresh.resolve(store.user.userSettings.data);
    });
    expect(screen.getByRole("heading", { name: "What would you like to use your space for?" })).toBeInTheDocument();

    const completion = deferred<void>();
    mocks.onboard.mockReturnValueOnce(completion.promise);
    await act(async () => {
      fireEvent.click(screen.getByRole("button", { name: "Skip and open my space" }));
    });
    expect(screen.getByRole("heading", { name: "What would you like to use your space for?" })).toBeInTheDocument();
    expect(mocks.replace).not.toHaveBeenCalled();

    await act(async () => {
      completion.resolve();
    });
    expect(mocks.onboard).toHaveBeenCalledOnce();
    expect(mocks.replace).toHaveBeenCalledWith("/personal-49bb21d9");
  });

  it("applies scalar profile updates when the mutation has no response body", async () => {
    await store.user.fetchCurrentUser();
    mocks.updateProfile.mockResolvedValueOnce(undefined);

    await store.user.userProfile.updateUserProfile({ lastWorkspaceId: workspace.id, isOnboarded: true });

    expect(store.user.userProfile.data.lastWorkspaceId).toBe(workspace.id);
    expect(store.user.userProfile.data.isOnboarded).toBe(true);
    expect(store.user.userProfile.data.onboardingStep).toEqual(profile.onboardingStep);
  });

  it("preserves the explicitly selected workspace when completing onboarding with multiple memberships", async () => {
    const earlierWorkspace = { ...workspace, id: "earlier-workspace", slug: "earlier" };
    mocks.workspaces.mockResolvedValue([earlierWorkspace, workspace]);
    await store.user.fetchCurrentUser();
    await store.user.userProfile.updateUserProfile({ lastWorkspaceId: workspace.id });

    await store.user.userProfile.finishUserOnboarding();
    mount();

    expect(store.user.userProfile.data.lastWorkspaceId).toBe(workspace.id);
    expect(mocks.replace).toHaveBeenCalledWith("/turtles");
  });

  it("waits for onboarding persistence, then uses the created workspace even with stale empty settings", async () => {
    await store.user.fetchCurrentUser();
    mount();
    const completion = deferred<void>();
    mocks.onboard.mockReturnValueOnce(completion.promise);

    await act(async () => {
      const created = await store.workspace.createWorkspace({ name: workspace.name, slug: workspace.slug });
      await store.workspace.fetchWorkspaces();
      await store.user.userProfile.updateUserProfile({ lastWorkspaceId: created.id });
      await store.user.userSettings.fetchCurrentUserSettings(true);
    });
    expect(store.workspace.getWorkspaceById(workspace.id)).toEqual(workspace);
    expect(store.user.userProfile.data.lastWorkspaceId).toBe(workspace.id);

    const settingsRefresh = deferred<IUserSettings>();
    mocks.settings.mockReturnValueOnce(settingsRefresh.promise);
    let finishing!: Promise<void>;
    await act(async () => {
      finishing = store.user.userProfile.finishUserOnboarding();
    });
    expect(mocks.replace).not.toHaveBeenCalled();
    expect(mocks.push).not.toHaveBeenCalled();

    await act(async () => {
      completion.resolve();
    });
    expect(store.user.userProfile.isLoading).toBe(false);
    expect(store.user.userProfile.isFinishingOnboarding).toBe(true);
    expect(mocks.replace).not.toHaveBeenCalled();

    await act(async () => {
      settingsRefresh.resolve(store.user.userSettings.data);
      await finishing;
    });
    expect(store.user.userProfile.data.isOnboarded).toBe(true);
    expect(mocks.settings).toHaveBeenLastCalledWith(true);
    expect(mocks.replace).toHaveBeenCalledWith("/turtles");
    expect(mocks.replace).not.toHaveBeenCalledWith("/create-workspace");
  });

  it("does not treat a pending authoritative workspace fetch as an empty list for an authenticated user", async () => {
    await store.user.fetchCurrentUser();
    profile = { ...profile, isOnboarded: true };
    await store.user.userProfile.fetchUserProfile();
    const pending = deferred<IWorkspace[]>();
    mocks.workspaces.mockReturnValueOnce(pending.promise);
    const loading = store.workspace.fetchWorkspaces();
    mount();
    expect(screen.getByText("Page content")).toBeInTheDocument();
    expect(mocks.replace).not.toHaveBeenCalled();
    await act(async () => {
      pending.resolve([workspace]);
      await loading;
    });
    expect(mocks.replace).toHaveBeenCalledWith("/turtles");
  });

  it("redirects an onboarded user when the successful workspace load is genuinely empty", async () => {
    profile = { ...profile, isOnboarded: true };
    await store.user.fetchCurrentUser();
    mount();
    expect(mocks.replace).toHaveBeenCalledWith("/create-workspace");
  });

  it("does not classify an uninitialized or failed workspace load as empty", async () => {
    const initial = store.workspace;
    profile = { ...profile, isOnboarded: true };
    await store.user.fetchCurrentUser();
    store.workspace = new WorkspaceStore(store);
    expect(initial.workspaces).toEqual({});
    const view = mount();
    expect(mocks.replace).not.toHaveBeenCalled();
    view.unmount();
    mocks.workspaces.mockRejectedValueOnce(new Error("offline"));
    await expect(store.workspace.fetchWorkspaces()).rejects.toThrow("offline");
    mount();
    expect(screen.getByRole("alert")).toHaveTextContent("Unable to load workspaces.");
    expect(mocks.replace).not.toHaveBeenCalled();
  });

  it.each([EPageType.NON_AUTHENTICATED, EPageType.SET_PASSWORD])(
    "uses the same authoritative workspace selector for %s",
    async (pageType) => {
      profile = { ...profile, isOnboarded: true };
      mocks.workspaces.mockResolvedValue([workspace]);
      await store.user.fetchCurrentUser();
      mount(pageType);
      expect(mocks.push).toHaveBeenCalledWith("/turtles");
      expect(mocks.push).not.toHaveBeenCalledWith("/create-workspace");
    }
  );

  it("renders a normal application route when an onboarded user has a workspace", async () => {
    profile = { ...profile, isOnboarded: true };
    mocks.workspaces.mockResolvedValue([workspace]);
    await store.user.fetchCurrentUser();
    mount(EPageType.AUTHENTICATED);
    expect(screen.getByText("Page content")).toBeInTheDocument();
    expect(mocks.push).not.toHaveBeenCalled();
    expect(mocks.replace).not.toHaveBeenCalled();
  });

  it("retains a created workspace when a pre-creation empty list request resolves late", async () => {
    await store.user.fetchCurrentUser();
    const old = deferred<IWorkspace[]>();
    mocks.workspaces.mockReturnValueOnce(old.promise);
    const loading = store.workspace.fetchWorkspaces();
    await store.workspace.createWorkspace({ slug: workspace.slug });
    old.resolve([]);
    await loading;
    profile = { ...profile, isOnboarded: true };
    await store.user.userProfile.fetchUserProfile();
    mount();
    expect(mocks.replace).toHaveBeenCalledWith("/turtles");
  });

  it("falls back from an invalid last slug to a valid fallback workspace", async () => {
    profile = { ...profile, isOnboarded: true };
    mocks.workspaces.mockResolvedValue([workspace]);
    mocks.settings.mockResolvedValue({
      ...store.user.userSettings.data,
      workspace: {
        ...store.user.userSettings.data.workspace,
        lastWorkspaceSlug: "removed",
        fallbackWorkspaceSlug: workspace.slug,
      },
    } satisfies IUserSettings);
    await store.user.fetchCurrentUser();
    mount();
    expect(mocks.replace).toHaveBeenCalledWith("/turtles");
  });

  it("first instance admin remains subject to ordinary web onboarding", async () => {
    mocks.currentUser.mockResolvedValue({ id: "user-1" } as IUser);
    await store.user.fetchCurrentUser();
    mount(EPageType.AUTHENTICATED);
    expect(mocks.push).toHaveBeenCalledWith("/onboarding");
    expect(store.workspace.workspaces).toEqual({});
  });

  it("keeps a legacy completed profile completed without an occupation", async () => {
    profile = { ...profile, role: undefined, isOnboarded: true };
    mocks.workspaces.mockResolvedValue([workspace]);
    await store.user.fetchCurrentUser();
    mount();
    expect(mocks.replace).toHaveBeenCalledWith("/turtles");
  });

  it("retains identity and renders retry after initial workspace bootstrap failure", async () => {
    mocks.workspaces.mockRejectedValueOnce(new Error("offline"));
    await store.user.fetchCurrentUser();
    mount();
    expect(store.user.data?.id).toBe("user-1");
    expect(screen.getByRole("alert")).toHaveTextContent("Unable to load workspaces.");
    expect(mocks.replace).not.toHaveBeenCalled();
  });

  it("does not reinterpret a failed revalidation of an empty loaded collection as confirmed empty", async () => {
    profile = { ...profile, isOnboarded: true };
    await store.user.fetchCurrentUser();
    mocks.workspaces.mockRejectedValueOnce(new Error("offline"));
    await expect(store.workspace.fetchWorkspaces()).rejects.toThrow("offline");
    mount();
    expect(screen.getByRole("alert")).toHaveTextContent("Unable to load workspaces.");
    expect(mocks.replace).not.toHaveBeenCalled();
  });

  it("refuses to persist new completion without a usable local destination", async () => {
    await store.user.fetchCurrentUser();
    await expect(store.user.userProfile.finishUserOnboarding()).rejects.toThrow("Choose a usable space");
    expect(mocks.onboard).not.toHaveBeenCalled();
  });

  it("propagates profile selection persistence failure instead of silently advancing", async () => {
    await store.user.fetchCurrentUser();
    mocks.updateProfile.mockRejectedValueOnce(new Error("offline"));
    await expect(store.user.userProfile.updateUserProfile({ lastWorkspaceId: workspace.id })).rejects.toThrow(
      "offline"
    );
    expect(store.user.userProfile.data.lastWorkspaceId).toBeUndefined();
  });

  it("does not persist legacy completed flags when canonical onboarding completion fails", async () => {
    await store.user.fetchCurrentUser();
    await store.workspace.createWorkspace({ name: workspace.name, slug: workspace.slug });
    mocks.onboard.mockRejectedValueOnce(new Error("completion unavailable"));
    await expect(store.user.userProfile.finishUserOnboarding()).rejects.toThrow("completion unavailable");
    expect(mocks.updateProfile).not.toHaveBeenCalled();
    expect(store.user.userProfile.data.isOnboarded).toBe(false);
    expect(store.user.userProfile.data.onboardingStep).toEqual(profile.onboardingStep);
    expect(store.user.userProfile.isFinishingOnboarding).toBe(false);
  });
});
