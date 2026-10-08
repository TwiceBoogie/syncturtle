import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { UserStore } from "./index";
import type { RootStore } from "@/syncturtle-web/store/root.store";
import type { IUser } from "@syncturtle/types";

const mocks = vi.hoisted(() => ({
  currentUser: vi.fn(),
  signOut: vi.fn(),
  clearCsrf: vi.fn(),
  resetProfile: vi.fn(),
  profile: vi.fn(),
  settings: vi.fn(),
}));
vi.mock("@/services/user.service", () => ({
  UserService: class {
    currentUser = mocks.currentUser;
  },
}));
vi.mock("@/services/auth.service", () => ({
  AuthService: class {
    signOut = mocks.signOut;
  },
}));
vi.mock("@/services/api.service", () => ({
  APIService: class {
    static clearSessionCsrf = mocks.clearCsrf;
  },
}));
vi.mock("./profile.store", () => ({
  ProfileStore: class {
    reset = mocks.resetProfile;
    fetchUserProfile = mocks.profile;
  },
}));
vi.mock("./settings.store", () => ({
  UserSettingsStore: class {
    fetchCurrentUserSettings = mocks.settings;
  },
}));
vi.mock("@syncturtle/utils", async () => ({
  ...(await import("../../../../../packages/utils/src/external-store")),
}));

/** This function creaes a controlled Promise where you can manually resolve or reject the promise from outside its scope */
function deferred<T>() {
  // ! = Typescript's definite assignment assertion operator
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((done, fail) => {
    resolve = done;
    reject = fail;
  });
  return { promise, resolve, reject };
}

const user = { id: "fictional-user" } as IUser;
let navigate: ReturnType<typeof vi.fn>;

function store() {
  return new UserStore({ workspace: { fetchWorkspaces: vi.fn().mockResolvedValue([]) } } as unknown as RootStore);
}

beforeEach(() => {
  vi.clearAllMocks();
  mocks.currentUser.mockReset().mockResolvedValue(user);
  mocks.signOut.mockReset().mockResolvedValue(undefined);
  mocks.profile.mockResolvedValue(undefined);
  mocks.settings.mockResolvedValue(undefined);
  navigate = vi.fn();
  vi.stubGlobal("window", { location: { pathname: "/workspace", assign: navigate } });
});
afterEach(() => {
  vi.unstubAllGlobals();
});

describe("one local signed-out transition", () => {
  it("clears auth/profile/CSRF and hard-navigates only once without network recovery", async () => {
    const current = store();
    await current.fetchCurrentUser();
    expect(current.isAuthenticated).toBe(true);
    current.transitionToSignedOut();
    current.transitionToSignedOut();
    expect(current.data).toBeUndefined();
    expect(current.isAuthenticated).toBe(false);
    expect(mocks.clearCsrf).toHaveBeenCalledTimes(1);
    expect(mocks.resetProfile).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledExactlyOnceWith("/");
    expect(mocks.signOut).not.toHaveBeenCalled();
  });

  it("settles an unauthenticated bootstrap on the sign-in route without reloading it", () => {
    const current = store();
    window.location.pathname = "/";

    current.transitionToSignedOut();
    current.transitionToSignedOut();

    expect(current.isAuthenticated).toBe(false);
    expect(current.data).toBeUndefined();
    expect(mocks.clearCsrf).toHaveBeenCalledTimes(1);
    expect(mocks.resetProfile).toHaveBeenCalledTimes(1);
    expect(navigate).not.toHaveBeenCalled();
  });

  it("reuses the local transition after explicit logout", async () => {
    const current = store();
    await current.fetchCurrentUser();
    await current.signOut();
    expect(mocks.signOut).toHaveBeenCalledTimes(1);
    expect(current.isAuthenticated).toBe(false);
    expect(navigate).toHaveBeenCalledExactlyOnceWith("/");
  });

  it("retains authenticated state when logout fails", async () => {
    const current = store();
    await current.fetchCurrentUser();
    mocks.signOut.mockRejectedValueOnce(new Error("unavailable"));
    await expect(current.signOut()).rejects.toThrow("unavailable");
    expect(current.isAuthenticated).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });

  it("does not let an older failed user fetch clear newer valid auth", async () => {
    const current = store();
    const old = deferred<IUser>();
    mocks.currentUser.mockReturnValueOnce(old.promise).mockResolvedValueOnce(user);
    const pending = current.fetchCurrentUser();
    const rejection = expect(pending).rejects.toThrow("late failure");
    await current.fetchCurrentUser();
    old.reject(new Error("late failure"));
    await rejection;
    expect(current.isAuthenticated).toBe(true);
    expect(current.error).toBeUndefined();
  });

  it("does not resurrect a user from an in-flight response after sign-out", async () => {
    const current = store();
    const old = deferred<IUser>();
    mocks.currentUser.mockReturnValueOnce(old.promise);
    const pending = current.fetchCurrentUser();
    current.transitionToSignedOut();
    old.resolve(user);
    await pending;
    expect(current.data).toBeUndefined();
    expect(current.isAuthenticated).toBe(false);
  });

  it("preserves existing auth on an operational user-fetch failure", async () => {
    const current = store();
    await current.fetchCurrentUser();
    mocks.currentUser.mockRejectedValueOnce(new Error("offline"));
    await expect(current.fetchCurrentUser()).rejects.toThrow("offline");
    expect(current.isAuthenticated).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });
});
