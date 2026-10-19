import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { IUser } from "@syncturtle/types";
import type { CoreRootStore } from "./root.store";

const mocks = vi.hoisted(() => ({
  signOut: vi.fn<() => Promise<void>>(),
}));
vi.mock("@/services/auth.service", () => ({
  AuthService: class {
    signOut = mocks.signOut;
    static signedOutDestination = () => "https://admin.test/god-mode/";
  },
}));
vi.mock("@syncturtle/constants", () => ({ API_BASE_URL: "https://admin.test" }));
vi.mock("@syncturtle/utils", () => import("../../../../packages/utils/src/external-store"));
const userService = vi.hoisted(() => ({ adminDetails: vi.fn<() => Promise<IUser>>() }));
vi.mock("@/services/user.service", () => ({
  UserService: class {
    adminDetails = userService.adminDetails;
  },
}));

const user = { id: "fictional-administrator" } as IUser;
let navigate: ReturnType<typeof vi.fn<(destination: string) => void>>;

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((done, fail) => {
    resolve = done;
    reject = fail;
  });
  return { promise, resolve, reject };
}

async function createStore() {
  const { UserStore } = await import("./user.store");
  const admins = vi.fn<() => Promise<void>>().mockResolvedValue(undefined);
  const reset = vi.fn<() => void>(() => current.reset());
  const root = { instance: { fetchInstanceAdmins: admins }, resetOnSignOut: reset } as unknown as CoreRootStore;
  const current = new UserStore(root);
  return { current, admins, reset };
}

beforeEach(() => {
  vi.clearAllMocks();
  mocks.signOut.mockReset().mockResolvedValue(undefined);
  userService.adminDetails.mockReset().mockResolvedValue(user);
  navigate = vi.fn<(destination: string) => void>();
  vi.stubGlobal("window", { location: { origin: "https://admin.test", assign: navigate } });
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("administrator store authentication lifecycle", () => {
  it("clears user and dependent state and navigates once without recursively calling logout", async () => {
    const { current, reset } = await createStore();
    await current.fetchCurrentUser();
    expect(current.isUserLoggedIn).toBe(true);
    current.transitionToSignedOut();
    current.transitionToSignedOut();
    expect(current.currentUser).toBeUndefined();
    expect(current.isUserLoggedIn).toBe(false);
    expect(current.isLoading).toBe(false);
    expect(reset).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledExactlyOnceWith("https://admin.test/god-mode/");
    expect(mocks.signOut).not.toHaveBeenCalled();
  });

  it("joins concurrent explicit logout and transitions only after the confirmed redirect", async () => {
    const { current, reset } = await createStore();
    await current.fetchCurrentUser();
    const logout = deferred<void>();
    mocks.signOut.mockReturnValueOnce(logout.promise);
    const firstSettled = vi.fn();
    const secondSettled = vi.fn();

    const first = current.signOut();
    const second = current.signOut();
    const firstObserved = first.then(firstSettled, firstSettled);
    const secondObserved = second.then(secondSettled, secondSettled);
    // Allow queued settlement callbacks to run while server confirmation is pending.
    await Promise.resolve();
    expect(firstSettled).not.toHaveBeenCalled();
    expect(secondSettled).not.toHaveBeenCalled();
    expect(mocks.signOut).toHaveBeenCalledTimes(1);
    expect(current.isUserLoggedIn).toBe(true);
    expect(current.currentUser).toEqual(user);
    expect(reset).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();

    logout.resolve();
    await expect(Promise.all([first, second])).resolves.toEqual([undefined, undefined]);
    await Promise.all([firstObserved, secondObserved]);
    expect(firstSettled).toHaveBeenCalledExactlyOnceWith(undefined);
    expect(secondSettled).toHaveBeenCalledExactlyOnceWith(undefined);
    await current.signOut();
    expect(current.isUserLoggedIn).toBe(false);
    expect(current.currentUser).toBeUndefined();
    expect(reset).toHaveBeenCalledTimes(1);
    expect(mocks.signOut).toHaveBeenCalledTimes(1);
    expect(navigate).toHaveBeenCalledExactlyOnceWith("https://admin.test/god-mode/");
  });

  it("retains local auth after an unconfirmed logout", async () => {
    const { current } = await createStore();
    await current.fetchCurrentUser();
    mocks.signOut.mockRejectedValueOnce(new Error("unconfirmed"));
    await expect(current.signOut()).rejects.toBeDefined();
    expect(current.isUserLoggedIn).toBe(true);
    expect(current.currentUser).toEqual(user);
    expect(navigate).not.toHaveBeenCalled();
    expect(mocks.signOut).toHaveBeenCalledTimes(1);
  });

  it("permits a later deliberate logout after uncertainty without an automatic retry", async () => {
    const { current } = await createStore();
    await current.fetchCurrentUser();
    mocks.signOut.mockRejectedValueOnce(new TypeError("offline"));
    await expect(current.signOut()).rejects.toThrow("offline");
    expect(mocks.signOut).toHaveBeenCalledTimes(1);
    await current.signOut();
    expect(mocks.signOut).toHaveBeenCalledTimes(2);
    expect(navigate).toHaveBeenCalledTimes(1);
  });

  it("suppresses a bootstrap response received after sign-out", async () => {
    const { current, admins } = await createStore();
    const old = deferred<IUser>();
    const started = deferred<void>();
    userService.adminDetails.mockImplementationOnce(() => {
      started.resolve();
      return old.promise;
    });
    const pending = current.fetchCurrentUser();
    await started.promise;
    current.transitionToSignedOut();
    old.resolve(user);
    await pending;
    expect(current.currentUser).toBeUndefined();
    expect(current.isUserLoggedIn).toBe(false);
    expect(admins).not.toHaveBeenCalled();
    await expect(current.fetchCurrentUser()).rejects.toThrow("session has ended");
    expect(userService.adminDetails).toHaveBeenCalledTimes(1);
  });

  it("does not resurrect user state after dependent admin hydration completes", async () => {
    const { current, admins } = await createStore();
    const hydration = deferred<void>();
    const started = deferred<void>();
    admins.mockImplementationOnce(() => {
      started.resolve();
      return hydration.promise;
    });
    const pending = current.fetchCurrentUser();
    await started.promise;
    current.transitionToSignedOut();
    hydration.resolve();
    await pending;
    expect(current.isUserLoggedIn).toBe(false);
    expect(current.currentUser).toBeUndefined();
  });

  it("ignores an older failed bootstrap after a newer valid user", async () => {
    const { current } = await createStore();
    const old = deferred<IUser>();
    const started = deferred<void>();
    userService.adminDetails.mockImplementationOnce(() => {
      started.resolve();
      return old.promise;
    });
    const pending = current.fetchCurrentUser();
    const rejected = expect(pending).rejects.toThrow("late failure");
    await started.promise;
    await current.fetchCurrentUser();
    old.reject(new Error("late failure"));
    await rejected;
    expect(current.currentUser).toEqual(user);
    expect(current.error).toBeUndefined();
  });

  it("retains existing auth during an operational bootstrap failure", async () => {
    const { current } = await createStore();
    await current.fetchCurrentUser();
    userService.adminDetails.mockRejectedValueOnce(new TypeError("offline"));
    await expect(current.fetchCurrentUser()).rejects.toThrow("offline");
    expect(current.isUserLoggedIn).toBe(true);
    expect(navigate).not.toHaveBeenCalled();
  });
});
