import type { IUserLite } from "@syncturtle/types";
import {
  WorkspaceMemberStore,
  type IWorkspaceMemberStore,
  type IWorkspaceMemberStoreInternal,
} from "./workspace/workspace-member.store";
import { ExternalStore } from "@syncturtle/utils";
import type { CoreRootStore } from "../root.store";

type TMemberRootSnapshot = {
  memberMap: Record<string, IUserLite>;
  revision: number;
};

const createInitialSnapshot = (): TMemberRootSnapshot => ({
  memberMap: {},
  revision: 0,
});

export interface IMemberRootStore {
  // observables
  memberMap: Record<string, IUserLite>;
  // computed actions / derived selectors
  getMemberIds: () => string[];
  getUserDetails: (userId: string) => IUserLite | undefined;
  // sub-stores
  workspace: IWorkspaceMemberStore;
}

export interface IMemberRootInternalStore extends IMemberRootStore {
  _subscribe: ExternalStore<TMemberRootSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TMemberRootSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TMemberRootSnapshot>["_getServerSnapshot"];

  _memberMapVersion: number;
  _upsertMember: (member: IUserLite) => void;
  _upsertManyMembers: (members: IUserLite[]) => void;
  _batch: (fn: () => void) => void;
}

export class MemberRootStore extends ExternalStore<TMemberRootSnapshot> implements IMemberRootInternalStore {
  public readonly workspace: IWorkspaceMemberStoreInternal;

  private readonly unsubscribers: Array<() => void> = [];

  private memberMapVersion = 0;
  private memberIdsCache = {
    version: -1,
    ids: [] as string[],
  };

  constructor(private readonly rootStore: CoreRootStore) {
    super(createInitialSnapshot());

    this.workspace = new WorkspaceMemberStore(this, rootStore);

    /**
     * Bridge nested workspace store updates back to MemberRootStore.
     *
     * useMember() subscribes to MemberRootStore only. So any nested store
     * that should affect useMember() consumers must eventually cause
     * MemberRootStore to emit.
     */
    this.unsubscribers.push(
      this.workspace._subscribe(() => {
        this.bumpRevision();
      })
    );
  }

  // raw getters
  public get memberMap(): Record<string, IUserLite> {
    return this.state.memberMap;
  }

  public getMemberIds = (): string[] => {
    if (this.memberIdsCache.version === this.memberMapVersion) {
      return this.memberIdsCache.ids;
    }

    // Object.keys(...) creates a new array every time so its cached
    const ids = Object.keys(this.state.memberMap);

    this.memberIdsCache = {
      version: this.memberMapVersion,
      ids,
    };

    return ids;
  };

  public getUserDetails = (userId: string): IUserLite | undefined => {
    if (!userId) return undefined;

    return this.state.memberMap[userId];
  };

  public get _memberMapVersion(): number {
    return this.memberMapVersion;
  }

  public _batch = (fn: () => void): void => {
    this.batch(fn);
  };

  public _upsertMember = (member: IUserLite): void => {
    if (!member?.id) return;

    const existing = this.state.memberMap[member.id];
    if (Object.is(existing, member)) return;

    this.replaceMemberMap({
      ...this.state.memberMap,
      [member.id]: member,
    });
  };

  public _upsertManyMembers = (members: IUserLite[]): void => {
    if (!members.length) return;

    const next = { ...this.state.memberMap };
    let changed = false;

    for (const member of members) {
      if (!member?.id) continue;

      if (!Object.is(next[member.id], member)) {
        next[member.id] = member;
        changed = true;
      }
    }

    if (!changed) return;

    this.replaceMemberMap(next);
  };

  public dispose = (): void => {
    this.unsubscribers.forEach((unsubscribe) => unsubscribe());
    this.unsubscribers.length = 0;
    this.workspace.dispose();
  };

  private replaceMemberMap(memberMap: Record<string, IUserLite>): void {
    if (this.shallowRecordEqual(this.state.memberMap, memberMap)) return;

    this.memberMapVersion++;
  }

  private bumpRevision(): void {
    this.setState((prev) => ({
      ...prev,
      revision: prev.revision + 1,
    }));
  }

  private shallowRecordEqual<T extends Record<string, unknown>>(a: T, b: T): boolean {
    if (Object.is(a, b)) return true;

    const aKeys = Object.keys(a);
    const bKeys = Object.keys(b);

    if (aKeys.length !== bKeys.length) return false;

    for (const key of aKeys) {
      if (!Object.prototype.hasOwnProperty.call(b, key)) return false;
      if (!Object.is(a[key], b[key])) return false;
    }

    return true;
  }
}
