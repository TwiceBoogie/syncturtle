import { WorkspaceService } from "@/services/workspace.service";
import type { IRouterStore } from "@/store/router.store";
import type { IUserStore } from "@/store/user";
import type { IUserLite, IWorkspaceMember, IWorkspaceMemberInvitation, TUserPermissions } from "@syncturtle/types";
import { ExternalStore } from "@syncturtle/utils";
import type { IMemberRootInternalStore } from "..";
import type { CoreRootStore } from "@/store/root.store";
import {
  WorkspaceMemberFiltersStore,
  type IWorkspaceMemberFiltersStore,
  type IWorkspaceMemberFiltersStoreInternal,
} from "./workspace-member-filters.store";
import { sortBy } from "lodash";

type TError = {
  status: string;
  message: string;
};

type TWorkspaceMemberSnapshot = {
  workspaceMemberMap: Record<string, Record<string, IWorkspaceMembership>>;
  workspaceMemberInvitations: Record<string, IWorkspaceMemberInvitation[]>;
  error: TError | undefined;
  revision: number;
};

const createInitialSnapshot = (): TWorkspaceMemberSnapshot => ({
  workspaceMemberMap: {},
  workspaceMemberInvitations: {},
  error: undefined,
  revision: 0,
});

export interface IWorkspaceMembership {
  id: string;
  member: string;
  role: TUserPermissions;
  isActive?: boolean;
}

export interface IWorkspaceMemberStore {
  // observables
  workspaceMemberMap: Record<string, Record<string, IWorkspaceMembership>>;
  workspaceMemberInvitations: Record<string, IWorkspaceMemberInvitation[]>;
  // filters store
  filtersStore: IWorkspaceMemberFiltersStore;
  // computed
  workspaceMemberIds: string[] | null;
  workspaceMemberInvitationIds: string[] | null;
  memberMap: Record<string, IWorkspaceMembership> | null;
  // computed actions / derived selectors
  getWorkspaceMemberIds: (workspaceSlug: string) => string[];
  getFilteredWorkspaceMemberIds: (workspaceSlug: string) => string[];
  getSearchedWorkspaceMemberIds: (searchQuery: string) => string[] | null;
  getSearchedWorkspaceInvitationIds: (searchQuery: string) => string[] | null;
  getWorkspaceMemberDetails: (workspaceMemberId: string) => IWorkspaceMember | null;
  getWorkspaceInvitationDetails: (invitationId: string) => IWorkspaceMemberInvitation | null;
  // fetch actions
  fetchWorkspaceMembers: (workspaceSlug: string) => Promise<IWorkspaceMember[]>;
}

export interface IWorkspaceMemberStoreInternal extends IWorkspaceMemberStore {
  _subscribe: ExternalStore<TWorkspaceMemberSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TWorkspaceMemberSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TWorkspaceMemberSnapshot>["_getServerSnapshot"];

  dispose: () => void;
}

export class WorkspaceMemberStore
  extends ExternalStore<TWorkspaceMemberSnapshot>
  implements IWorkspaceMemberStoreInternal
{
  public readonly filtersStore: IWorkspaceMemberFiltersStoreInternal;

  private readonly workspaceService: WorkspaceService;
  private readonly routerStore: IRouterStore;
  private readonly userStore: IUserStore;
  private readonly unsubscribers: Array<() => void> = [];

  private workspaceMemberMapVersion = 0;
  private workspaceMemberInvitationVersion = 0;

  private fetchMembersRequestSeq = 0;
  private fetchInvitationsRequestSeq = 0;

  private readonly emptyWorkspaceMemberMap: Record<string, IWorkspaceMembership> = {};

  private workspaceMemberIdsCache = new Map<string, string[]>();
  private filteredWorkspaceMemberIdsCache = new Map<string, string[]>();
  private searchedWorkspaceMemberIdsCache = new Map<string, string[] | null>();
  private workspaceMemberDetailsCache = new Map<string, IWorkspaceMember | null>();

  private workspaceMemberInvitationIdsCache = new Map<string, string[] | null>();
  private searchedWorkspaceInvitationIdsCache = new Map<string, string[] | null>();

  constructor(
    private readonly memberRoot: IMemberRootInternalStore,
    _rootStore: CoreRootStore
  ) {
    super(createInitialSnapshot());

    this.routerStore = _rootStore.router;
    this.userStore = _rootStore.user;
    this.workspaceService = new WorkspaceService();

    this.filtersStore = new WorkspaceMemberFiltersStore();

    this.unsubscribers.push(
      this.filtersStore._subscribe(() => {
        this.clearWorkspaceMemberFilterCaches();
        this.bumpRevision();
      })
    );
  }

  // raw getters
  public get workspaceMemberMap(): Record<string, Record<string, IWorkspaceMembership>> {
    return this.state.workspaceMemberMap;
  }

  public get workspaceMemberInvitations(): Record<string, IWorkspaceMemberInvitation[]> {
    return this.state.workspaceMemberInvitations;
  }

  public get error(): TError | undefined {
    return this.state.error;
  }

  // computed
  public get workspaceMemberIds(): string[] | null {
    const workspaceSlug = this.routerStore.workspaceSlug;
    if (!workspaceSlug) return null;

    return this.getWorkspaceMemberIds(workspaceSlug);
  }

  public get workspaceMemberInvitationIds(): string[] | null {
    const workspaceSlug = this.routerStore.workspaceSlug;
    if (!workspaceSlug) return null;

    return this.getWorkspaceMemberInvitationIds(workspaceSlug);
  }

  public get memberMap(): Record<string, IWorkspaceMembership> | null {
    const workspaceSlug = this.routerStore.workspaceSlug;
    if (!workspaceSlug) return null;

    return this.state.workspaceMemberMap[workspaceSlug] ?? {};
  }

  // computed actions
  public getWorkspaceMemberIds = (workspaceSlug: string): string[] => {
    if (!workspaceSlug) return [];

    const currentUserId = this.userStore.data?.id ?? "";
    const cacheKey = [
      workspaceSlug,
      currentUserId,
      this.workspaceMemberMapVersion,
      this.memberRoot._memberMapVersion,
    ].join(":");

    const cached = this.workspaceMemberIdsCache.get(cacheKey);
    if (cached) return cached;

    let members = Object.values(this.state.workspaceMemberMap[workspaceSlug] ?? {});

    members = sortBy(members, [
      (membership) => membership.member !== currentUserId,
      (membership) => this.getMemberDisplayName(this.memberRoot.memberMap[membership.member]),
    ]);

    const ids = members
      .filter((membership) => {
        const member = this.memberRoot.memberMap[membership.member];
        return !this.isBot(member);
      })
      .map((membership) => membership.member);

    this.workspaceMemberIdsCache.set(cacheKey, ids);

    return ids;
  };

  public getFilteredWorkspaceMemberIds = (workspaceSlug: string): string[] => {
    if (!workspaceSlug) return [];

    const cacheKey = [
      workspaceSlug,
      this.workspaceMemberMapVersion,
      this.memberRoot._memberMapVersion,
      this.filtersStore.version,
    ].join(":");

    const cached = this.filteredWorkspaceMemberIdsCache.get(cacheKey);
    if (cached) return cached;

    const members = Object.values(this.state.workspaceMemberMap[workspaceSlug] ?? {}).filter((membership) => {
      const member = this.memberRoot.memberMap[membership.member];
      return !this.isBot(member);
    });

    const ids = this.filtersStore.getFilteredMemberIds(
      members,
      this.memberRoot.memberMap,
      (membership) => membership.member
    );

    this.filteredWorkspaceMemberIdsCache.set(cacheKey, ids);

    return ids;
  };

  public getSearchedWorkspaceMemberIds = (searchQuery: string): string[] | null => {
    const workspaceSlug = this.routerStore.workspaceSlug;
    if (!workspaceSlug) return null;

    const normalizedSearchQuery = searchQuery.trim().toLowerCase();

    const cacheKey = [
      workspaceSlug,
      normalizedSearchQuery,
      this.workspaceMemberMapVersion,
      this.memberRoot._memberMapVersion,
      this.filtersStore.version,
    ].join(":");

    const cached = this.searchedWorkspaceMemberIdsCache.get(cacheKey);
    if (cached !== undefined) return cached;

    const filteredIds = this.getFilteredWorkspaceMemberIds(workspaceSlug);

    const ids = filteredIds.filter((userId) => {
      const details = this.getWorkspaceMemberDetails(userId);
      if (!details?.member) return false;

      const member: IUserLite = details.member;

      const searchableText = [member.firstName, member.lastName, member.displayName, member.email]
        .filter(Boolean)
        .join(":")
        .toLowerCase();

      return searchableText.includes(normalizedSearchQuery);
    });

    this.searchedWorkspaceMemberIdsCache.set(cacheKey, ids);

    return ids;
  };

  public getSearchedWorkspaceInvitationIds = (searchQuery: string): string[] | null => {
    const workspaceSlug = this.routerStore.workspaceSlug;
    if (!workspaceSlug) return null;

    const normalizedSearchQuery = searchQuery.trim().toLowerCase();

    const cacheKey = [workspaceSlug, normalizedSearchQuery, this.workspaceMemberInvitationVersion].join(":");

    const cached = this.searchedWorkspaceInvitationIdsCache.get(cacheKey);
    if (cached !== undefined) return cached;

    const invitationIds = this.getWorkspaceMemberInvitationIds(workspaceSlug);

    if (!invitationIds) {
      this.searchedWorkspaceInvitationIdsCache.set(cacheKey, null);
      return null;
    }

    const ids = invitationIds.filter((invitationId) => {
      const invitation = this.getWorkspaceInvitationDetails(invitationId);
      if (!invitation) return false;

      return invitation.email.toLowerCase().includes(normalizedSearchQuery);
    });

    this.searchedWorkspaceInvitationIdsCache.set(cacheKey, ids);

    return ids;
  };

  public getWorkspaceMemberDetails = (userId: string): IWorkspaceMember | null => {
    const workspaceSlug = this.routerStore.workspaceSlug;
    if (!workspaceSlug || !userId) return null;

    const cacheKey = [workspaceSlug, userId, this.workspaceMemberMapVersion, this.memberRoot._memberMapVersion].join(
      ":"
    );

    const cached = this.workspaceMemberDetailsCache.get(cacheKey);
    if (cached !== undefined) return cached;

    const workspaceMember = this.state.workspaceMemberMap[workspaceSlug]?.[userId];

    if (!workspaceMember) {
      this.workspaceMemberDetailsCache.set(cacheKey, null);
      return null;
    }

    const member = this.memberRoot.memberMap[workspaceMember.member];

    if (!member) {
      this.workspaceMemberDetailsCache.set(cacheKey, null);
      return null;
    }

    const details: IWorkspaceMember = {
      id: workspaceMember.id,
      role: workspaceMember.role,
      member,
      isActive: workspaceMember.isActive,
    };

    this.workspaceMemberDetailsCache.set(cacheKey, details);

    return details;
  };

  public getWorkspaceInvitationDetails = (invitationId: string): IWorkspaceMemberInvitation | null => {
    const workspaceSlug = this.routerStore.workspaceSlug;
    if (!workspaceSlug || !invitationId) return null;

    const invitations = this.state.workspaceMemberInvitations[workspaceSlug];

    return invitations?.find((invitation) => invitation.id === invitationId) ?? null;
  };

  public dispose = (): void => {
    this.unsubscribers.forEach((unsubscribe) => unsubscribe());
    this.unsubscribers.length = 0;
  };

  // fetch actions
  public fetchWorkspaceMembers = async (workspaceSlug: string): Promise<IWorkspaceMember[]> => {
    const requestSeq = ++this.fetchMembersRequestSeq;

    this.setState({ error: undefined });

    try {
      const response = await this.workspaceService.fetchWorkspaceMembers(workspaceSlug);

      if (requestSeq !== this.fetchMembersRequestSeq) {
        return response;
      }

      const members = response.map((workspaceMember) => workspaceMember.member).filter((member) => Boolean(member.id));

      const workspaceMembers = response.reduce<Record<string, IWorkspaceMembership>>((acc, workspaceMember) => {
        const membership: IWorkspaceMembership = {
          id: workspaceMember.id,
          member: workspaceMember.member.id,
          role: workspaceMember.role,
          isActive: workspaceMember.isActive,
        };

        acc[membership.member] = membership;
        return acc;
      }, {});

      this.memberRoot._batch(() => {
        this.memberRoot._upsertManyMembers(members);
      });

      this.replaceWorkspaceMembersForWorkspace(workspaceSlug, workspaceMembers);

      this.setState({ error: undefined });
      return response;
    } catch (error) {
      if (requestSeq === this.fetchMembersRequestSeq) {
        this.setState({
          error: {
            status: "fetch-workspace-members-error",
            message: "Failed to fetch workspace members",
          },
        });
      }

      throw error;
    }
  };

  private getWorkspaceMemberInvitationIds(workspaceSlug: string): string[] | null {
    const cacheKey = [workspaceSlug, this.workspaceMemberInvitationVersion].join(":");

    const cached = this.workspaceMemberInvitationIdsCache.get(cacheKey);
    if (cached !== undefined) return cached;

    const invitations = this.state.workspaceMemberInvitations[workspaceSlug];

    if (!invitations) {
      this.workspaceMemberInvitationIdsCache.set(cacheKey, null);
      return null;
    }

    const ids = invitations.map((invitation) => invitation.id);

    this.workspaceMemberInvitationIdsCache.set(cacheKey, ids);

    return ids;
  }

  private replaceWorkspaceMembersForWorkspace(
    workspaceSlug: string,
    members: Record<string, IWorkspaceMembership>
  ): void {
    const current = this.state.workspaceMemberMap[workspaceSlug] ?? {};

    if (this.shallowRecordEqual(current, members)) return;

    this.workspaceMemberMapVersion++;
    this.clearWorkspaceMemberCaches();

    this.setState({
      workspaceMemberMap: {
        ...this.state.workspaceMemberMap,
        [workspaceSlug]: members,
      },
    });
  }

  private clearWorkspaceMemberCaches(): void {
    this.workspaceMemberIdsCache.clear();
    this.filteredWorkspaceMemberIdsCache.clear();
    this.searchedWorkspaceInvitationIdsCache.clear();
    this.workspaceMemberDetailsCache.clear();
  }

  private clearWorkspaceMemberFilterCaches(): void {
    this.filteredWorkspaceMemberIdsCache.clear();
    this.searchedWorkspaceMemberIdsCache.clear();
  }

  private bumpRevision(): void {
    this.setState((prev) => ({
      ...prev,
      revision: prev.revision + 1,
    }));
  }

  private getMemberDisplayName(member: IUserLite | undefined): string {
    if (!member) return "";

    return member.displayName.toLowerCase();
  }

  private isBot(member: IUserLite | undefined): boolean {
    if (!member) return false;

    return member.isBot;
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
