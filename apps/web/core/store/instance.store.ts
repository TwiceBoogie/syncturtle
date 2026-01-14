import { InstanceService } from "@/services/instance.service";
import { IInstance, IInstanceConfig } from "@syncturtle/types";
import { ExternalStore } from "@syncturtle/utils";

type TError = {
  status: string;
  message: string;
  data?: {
    isActivated: boolean;
    isSetupDone: boolean;
  };
};

export type TInstanceSnapshot = {
  isLoading: boolean;
  instance: IInstance | undefined;
  config: IInstanceConfig | undefined;
  error: TError | undefined;
};

const createInitialSnapshot = (): TInstanceSnapshot => ({
  isLoading: false,
  instance: undefined,
  config: undefined,
  error: undefined,
});

export interface IInstanceStoreInternal {
  _subscribe: ExternalStore<TInstanceSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TInstanceSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TInstanceSnapshot>["_getServerSnapshot"];
  // observables
  isLoading: boolean;
  instance: IInstance | undefined;
  config: IInstanceConfig | undefined;
  error: TError | undefined;
  // actions
  fetchInstanceInfo: () => Promise<void>;
}

export type TInstanceStore = Omit<IInstanceStoreInternal, "_subscribe" | "_getSnapshot" | "_getServerSnapshot">;

export class InstanceStore extends ExternalStore<TInstanceSnapshot> implements IInstanceStoreInternal {
  private readonly instanceService: InstanceService;

  constructor() {
    super(createInitialSnapshot());

    this.instanceService = new InstanceService();
  }

  // raw getters for data
  get isLoading(): boolean {
    return this.state.isLoading;
  }

  get instance(): IInstance | undefined {
    return this.state.instance;
  }

  get config(): IInstanceConfig | undefined {
    return this.state.config;
  }

  get error(): TError | undefined {
    return this.state.error;
  }

  public fetchInstanceInfo = async (): Promise<void> => {
    this.setState({ isLoading: true, error: undefined });

    try {
      const instanceInfo = await this.instanceService.getInstanceInfo();
      this.setState({
        isLoading: false,
        instance: instanceInfo.instance,
        config: instanceInfo.config,
      });
    } catch (error) {
      this.setState({
        isLoading: false,
        error: {
          status: "error",
          message: "Failed to fetch instance info.",
        },
      });
      throw error;
    }
  };
}
