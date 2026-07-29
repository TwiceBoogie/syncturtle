import { WorkspaceService } from "@/services/workspace.service";
import { CoreRootStore } from "../root.store";
// local types
import type { IUserStore } from "../user";
import type { IRouterStore } from "../router.store";
// utils
import { ExternalStore } from "@syncturtle/utils";
// types
import type { IWorkspace } from "@syncturtle/types";

type TError = {
  status: string;
  message: string;
};

export type TWorkspaceSnapshot = {
  loader: boolean;
  workspaces: Record<string, IWorkspace>;
  error: TError | undefined;
};

const createInitialSnapshot = (): TWorkspaceSnapshot => ({
  loader: false,
  workspaces: {},
  error: undefined,
});

type TWorkspaceBySlugCache = {
  version: number;
  map: Map<string, IWorkspace>;
};

type TWorkspaceListCache = {
  version: number;
  list: IWorkspace[];
};

type TWorkspacesCreatedByUserCache = {
  version: number;
  userId: string | undefined;
  list: IWorkspace[];
};

export interface IWorkspaceStore {
  // observables
  loader: boolean;
  workspaces: Record<string, IWorkspace>;
  error: TError | undefined;
  // computed
  currentWorkspace: IWorkspace | null;
  workspacesCreatedByCurrentUser: IWorkspace[] | null;
  // computed actions
  getWorkspaceRedirectionUrl: () => string;
  getWorkspaceBySlug: (workspaceSlug: string) => IWorkspace | null;
  getWorkspaceById: (workspaceId: string) => IWorkspace | null;

  // actions
  fetchWorkspaces: () => Promise<IWorkspace[]>;
  createWorkspace: (data: Partial<IWorkspace>) => Promise<IWorkspace>;
  updateWorkspace: (workspaceSlug: string, data: Partial<IWorkspace>) => Promise<IWorkspace>;
  updateWorkspaceLogo: (workspaceSlug: string, logoURL: string) => void;
  deleteWorkspace: (workspaceSlug: string) => Promise<void>;
  // sub-stores
}

export interface IWorkspaceStoreInternal extends IWorkspaceStore {
  _subscribe: ExternalStore<TWorkspaceSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TWorkspaceSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TWorkspaceSnapshot>["_getServerSnapshot"];
}

export class WorkspaceStore extends ExternalStore<TWorkspaceSnapshot> implements IWorkspaceStoreInternal {
  private readonly workspaceService: WorkspaceService;
  private readonly router: IRouterStore;
  private readonly user: IUserStore;

  // computed cache (versioned)
  private workspacesVersion = 0;
  private workspaceListCache: TWorkspaceListCache = { version: -1, list: [] };
  private workspaceBySlugCache: TWorkspaceBySlugCache = { version: -1, map: new Map<string, IWorkspace>() };
  private workspacesCreatedByUserCache: TWorkspacesCreatedByUserCache = {
    version: -1,
    userId: undefined,
    list: [],
  };

  private fetchRequestSeq = 0;

  constructor(private readonly _store: CoreRootStore) {
    super(createInitialSnapshot());

    this.workspaceService = new WorkspaceService();

    this.router = _store.router;
    this.user = _store.user;
  }

  // State getters
  get loader(): boolean {
    return this.state.loader;
  }

  get workspaces(): Record<string, IWorkspace> {
    return this.state.workspaces;
  }

  get error(): TError | undefined {
    return this.state.error;
  }

  // Computed
  get currentWorkspace(): IWorkspace | null {
    const workspaceSlug = this.router.workspaceSlug;
    if (!workspaceSlug) return null;

    return this.getWorkspaceBySlug(workspaceSlug);
  }

  get workspacesCreatedByCurrentUser(): IWorkspace[] | null {
    const userId = this.user.data?.id;
    if (!userId) return null;

    if (
      this.workspacesCreatedByUserCache.version === this.workspacesVersion &&
      this.workspacesCreatedByUserCache.userId === userId
    ) {
      return this.workspacesCreatedByUserCache.list;
    }

    const list = this.workspaceList.filter((workspace) => workspace.owner.id === userId);

    this.workspacesCreatedByUserCache = {
      version: this.workspacesVersion,
      userId,
      list,
    };

    return list;
  }

  // Computed actions / selectors
  public getWorkspaceRedirectionUrl = (): string => {
    const currentWorkspaceSlug =
      this.user.userSettings?.data?.workspace?.lastWorkspaceSlug ||
      this.user.userSettings?.data?.workspace?.fallbackWorkspaceSlug;

    if (!currentWorkspaceSlug) return "/create-workspace";

    const workspace = this.getWorkspaceBySlug(currentWorkspaceSlug);
    if (!workspace) return "/create-workspace";

    return `/${currentWorkspaceSlug}`;
  };

  public getWorkspaceBySlug = (workspaceSlug: string): IWorkspace | null => {
    if (!workspaceSlug) return null;

    return this.workspaceBySlugMap.get(workspaceSlug) ?? null;
  };

  public getWorkspaceById = (workspaceId: string): IWorkspace | null => {
    if (!workspaceId) return null;

    return this.state.workspaces[workspaceId] ?? null;
  };

  // fetch actions
  public fetchWorkspaces = async (): Promise<IWorkspace[]> => {
    const requestSeq = ++this.fetchRequestSeq;

    this.setState({ loader: true, error: undefined });

    try {
      const workspaces = await this.workspaceService.userWorkspaces();
      // prevent an older/slower request from overwriting a newer request
      if (requestSeq !== this.fetchRequestSeq) return workspaces;

      this.batch(() => {
        this.upsertMany(workspaces);
        this.setState({ loader: false, error: undefined });
      });

      return workspaces;
    } catch (error) {
      if (requestSeq === this.fetchRequestSeq) {
        this.setState({
          loader: false,
          error: {
            status: "fetch-workspaces-error",
            message: "Failed to fetch workspaces",
          },
        });
      }
      throw error;
    }
  };

  // crud actions
  public createWorkspace = async (data: Partial<IWorkspace>): Promise<IWorkspace> => {
    try {
      const workspace = await this.workspaceService.createWorkspace(data);

      this.batch(() => {
        this.upsertOne(workspace);
        this.setState({ error: undefined });
      });

      return workspace;
    } catch (error) {
      this.setState({
        error: {
          status: "create-workspace-error",
          message: "Failed to create workspace",
        },
      });

      throw error;
    }
  };

  public updateWorkspace = async (workspaceSlug: string, data: Partial<IWorkspace>): Promise<IWorkspace> => {
    try {
      const response = await this.workspaceService.updateWorkspace(workspaceSlug, data);

      if (response?.id) {
        const existing = this.state.workspaces[response.id];

        const nextWorkspace = {
          ...existing,
          ...data,
          ...response,
        } as IWorkspace;

        this.batch(() => {
          this.upsertOne(nextWorkspace);
          this.setState({ error: undefined });
        });
      }

      return response;
    } catch (error) {
      this.setState({
        error: {
          status: "update-workspace-error",
          message: "Failed to update workspace",
        },
      });

      throw error;
    }
  };

  public updateWorkspaceLogo = (workspaceSlug: string, logoAssetId: string): void => {
    const workspace = this.getWorkspaceBySlug(workspaceSlug);

    if (!workspace?.id) {
      throw new Error("Workspace not found");
    }

    const nextWorkspace: IWorkspace = { ...workspace, logoAssetId };

    this.upsertOne(nextWorkspace);
  };

  public deleteWorkspace = async (workspaceSlug: string): Promise<void> => {
    try {
      await this.workspaceService.deleteWorkspace(workspaceSlug);

      this.batch(() => {
        this.removeWorkspaceBySlug(workspaceSlug);
        this.setState({ error: undefined });
      });
    } catch (error) {
      this.setState({
        error: {
          status: "delete-workspace-error",
          message: "Failed to delete workspace",
        },
      });

      throw error;
    }
  };

  // computed helpers
  private get workspaceList(): IWorkspace[] {
    if (this.workspaceListCache.version === this.workspacesVersion) {
      return this.workspaceListCache.list;
    }

    const list = Object.values(this.state.workspaces);

    this.workspaceListCache = {
      version: this.workspacesVersion,
      list,
    };

    return list;
  }

  private get workspaceBySlugMap(): Map<string, IWorkspace> {
    if (this.workspaceBySlugCache.version === this.workspacesVersion) {
      return this.workspaceBySlugCache.map;
    }

    const map = new Map<string, IWorkspace>();

    for (const workspace of Object.values(this.state.workspaces)) {
      if (workspace.slug) {
        map.set(workspace.slug, workspace);
      }
    }

    this.workspaceBySlugCache = {
      version: this.workspacesVersion,
      map,
    };

    return map;
  }

  // internal helpers
  private upsertOne(workspace: IWorkspace): void {
    if (!workspace?.id) return;

    const next = { ...this.state.workspaces, [workspace.id]: workspace };

    this.replaceWorkspaces(next);
  }

  private upsertMany(workspaces: IWorkspace[]): void {
    if (!workspaces.length) return;

    const next = { ...this.state.workspaces };
    let changed = false;

    for (const workspace of workspaces) {
      if (!workspace?.id) continue;

      if (!Object.is(next[workspace.id], workspace)) {
        next[workspace.id] = workspace;
        changed = true;
      }
    }

    if (!changed) return;

    this.replaceWorkspaces(next);
  }

  private removeWorkspaceBySlug(workspaceSlug: string): void {
    const workspace = this.getWorkspaceBySlug(workspaceSlug);
    if (!workspace?.id) return;

    const next = { ...this.state.workspaces };
    delete next[workspace.id];

    this.replaceWorkspaces(next);
  }

  private replaceWorkspaces(next: Record<string, IWorkspace>): void {
    if (this.shallowRecordEqual(this.state.workspaces, next)) return;

    this.bumpWorkspacesVersion();

    this.setState({ workspaces: next });
  }

  private bumpWorkspacesVersion(): void {
    this.workspacesVersion++;

    this.workspaceListCache.version = -1;
    this.workspaceBySlugCache.version = -1;
    this.workspacesCreatedByUserCache.version = -1;
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
