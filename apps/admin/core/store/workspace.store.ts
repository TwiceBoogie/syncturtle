import { WorkspaceService } from "@/services/workspace.service";
import { IWorkspace, TLoader, TPaginationInfo } from "@syncturtle/types";
import { ExternalStore } from "@syncturtle/utils";
import { CoreRootStore } from "./root.store";

type TError = {
  status: string;
  message: string;
};

export type TWorkspaceSnapshot = {
  loader: TLoader;
  workspaces: Record<string, IWorkspace>;
  paginationInfo: TPaginationInfo | undefined;
  error: TError | undefined;
};

const createInitialSnapshot = (): TWorkspaceSnapshot => ({
  loader: "init-loader",
  workspaces: {},
  paginationInfo: undefined,
  error: undefined,
});

export interface IWorkspaceStoreInternal {
  _subscribe: ExternalStore<TWorkspaceSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TWorkspaceSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TWorkspaceSnapshot>["_getServerSnapshot"];
  // observables
  loader: TLoader;
  workspaces: Record<string, IWorkspace>;
  paginationInfo: TPaginationInfo | undefined;
  // computed
  workspaceIds: string[];
  // helper actions
  getWorkspaceById: (workspaceId: string) => IWorkspace | undefined;
  // actions
  fetchWorkspaces: () => Promise<IWorkspace[]>;
  fetchNextWorkspaces: () => Promise<IWorkspace[]>;
  createWorkspace: (data: Partial<IWorkspace>) => Promise<IWorkspace>;
}

export type TWorkspaceStore = Omit<IWorkspaceStoreInternal, "_subscribe" | "_getSnapshot" | "_getServerSnapshot">;

export class WorkspaceStore extends ExternalStore<TWorkspaceSnapshot> implements IWorkspaceStoreInternal {
  private readonly workspaceService: WorkspaceService;

  // comuted cache (versioned);
  private workspacesVersion = 0;
  private workspaceIdsCache = { version: -1, ids: [] as string[] };

  constructor(private readonly _store: CoreRootStore) {
    super(createInitialSnapshot());

    this.workspaceService = new WorkspaceService();
  }

  // raw getters for data
  get loader(): TLoader {
    return this.state.loader;
  }

  get workspaces(): Record<string, IWorkspace> {
    return this.state.workspaces;
  }

  get paginationInfo(): TPaginationInfo | undefined {
    return this.state.paginationInfo;
  }

  get error(): TError | undefined {
    return this.state.error;
  }

  // computed
  get workspaceIds(): string[] {
    if (this.workspaceIdsCache.version === this.workspacesVersion) {
      return this.workspaceIdsCache.ids;
    }

    const ids = Object.keys(this.state.workspaces);
    // update cache
    this.workspaceIdsCache = { version: this.workspacesVersion, ids };
    return ids;
  }

  // helper actions
  public getWorkspaceById = (workspaceId: string): IWorkspace | undefined => this.state.workspaces[workspaceId];

  // actions
  public fetchWorkspaces = async (): Promise<IWorkspace[]> => {
    if (this.workspaceIds.length > 0) {
      this.setState({ loader: "mutation" });
    } else {
      this.setState({ loader: "init-loader" });
    }

    try {
      const response = await this.workspaceService.list();
      const { results, ...paginationInfo } = response;

      this.batch(() => {
        this.upsertMany(results);
        this.setState({ paginationInfo: paginationInfo });
        this.setState({ loader: "loaded", error: undefined });
      });

      return results;
    } catch (error) {
      this.setState({
        loader: "loaded",
        error: {
          status: "fetch-workspaces-error",
          message: "Failed to fetch workspaces",
        },
      });
      throw error;
    }
  };

  public fetchNextWorkspaces = async (): Promise<IWorkspace[]> => {
    if (!this.paginationInfo || this.paginationInfo.nextPageResults === false) return [];
    this.setState({ loader: "pagination", error: undefined });
    try {
      const response = await this.workspaceService.list(this.paginationInfo.nextCursor);
      const { results, ...paginationInfo } = response;

      this.batch(() => {
        this.upsertMany(results);
        this.setState({ paginationInfo: paginationInfo });
        this.setState({ loader: "loaded" });
      });

      return results;
    } catch (error) {
      this.setState({
        loader: "loaded",
        error: {
          status: "fetch-next-workspaces-error",
          message: "Failed to fetch next workspaces",
        },
      });
      throw error;
    }
  };

  public createWorkspace = async (data: Partial<IWorkspace>): Promise<IWorkspace> => {
    this.setState({ loader: "mutation", error: undefined });

    try {
      const workspace = await this.workspaceService.create(data);
      this.batch(() => {
        this.upsertOne(workspace);
        this.setState({ loader: "loaded" });
      });

      return workspace;
    } catch (error) {
      this.setState({
        loader: "loaded",
        error: {
          status: "create-workspace-error",
          message: "Failed to create workspace",
        },
      });
      throw error;
    }
  };

  // internal helpers
  private bumpWorkspacesVersion(): void {
    this.workspacesVersion++;
    // invalidate computed cache
    this.workspaceIdsCache.version = -1;
  }

  private upsertOne = (workspace: IWorkspace) => {
    if (!workspace?.id) return;

    this.bumpWorkspacesVersion();
    this.setState((prev) => ({
      ...prev,
      workspaces: {
        ...prev.workspaces,
        [workspace.id]: workspace,
      },
    }));
  };

  private upsertMany = (workspaces: IWorkspace[]) => {
    if (!workspaces?.length) return;

    this.bumpWorkspacesVersion();
    this.setState((prev) => {
      const next = { ...prev.workspaces };
      for (const w of workspaces) {
        if (!w?.id) continue;
        next[w.id] = w;
      }
      return { ...prev, workspaces: next };
    });
  };
}
