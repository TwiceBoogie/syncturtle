import type { IUserLite, TUserPermissions } from "@syncturtle/types";
import { sortWorkspaceMembers, type IMemberFilters } from "../utils";
import { ExternalStore } from "@syncturtle/utils";

interface IWorkspaceMembership {
  id: string;
  member: string;
  role: TUserPermissions;
  isActive?: boolean;
}

type TWorkspaceMemberFiltersSnapshot = {
  filters: IMemberFilters;
};

const createInitialSnapshot = (): TWorkspaceMemberFiltersSnapshot => ({
  filters: {},
});

export interface IWorkspaceMemberFiltersStore {
  // observables
  filters: IMemberFilters;
  version: number;
  // computed actions
  getFilteredMemberIds: (
    members: IWorkspaceMembership[],
    memberDetailsMap: Record<string, IUserLite>,
    getMemberKey: (member: IWorkspaceMembership) => string
  ) => string[];
  // actions
  updateFilters: (filters: Partial<IMemberFilters>) => void;
}

export interface IWorkspaceMemberFiltersStoreInternal extends IWorkspaceMemberFiltersStore {
  _subscribe: ExternalStore<TWorkspaceMemberFiltersSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TWorkspaceMemberFiltersSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TWorkspaceMemberFiltersSnapshot>["_getServerSnapshot"];
}

export class WorkspaceMemberFiltersStore
  extends ExternalStore<TWorkspaceMemberFiltersSnapshot>
  implements IWorkspaceMemberFiltersStoreInternal
{
  private filtersVersion = 0;

  constructor() {
    super(createInitialSnapshot());
  }

  // raw getters
  public get filters(): IMemberFilters {
    return this.state.filters;
  }

  public get version(): number {
    return this.filtersVersion;
  }

  // computed actions
  public getFilteredMemberIds = (
    members: IWorkspaceMembership[],
    memberDetailsMap: Record<string, IUserLite>,
    getMemberKey: (member: IWorkspaceMembership) => string
  ): string[] => {
    if (!members.length) return [];

    const sortedMembers = sortWorkspaceMembers(members, memberDetailsMap, getMemberKey, this.state.filters);

    return sortedMembers.map(getMemberKey);
  };

  public updateFilters = (filters: Partial<IMemberFilters>): void => {
    const nextFilters = {
      ...this.state.filters,
      ...filters,
    };

    if (this.shallowRecordEqual(this.state.filters, nextFilters)) return;

    this.filtersVersion++;

    this.setState({
      filters: nextFilters,
    });
  };

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
