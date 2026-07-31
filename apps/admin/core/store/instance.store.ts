import type {
  IInstance,
  IInstanceAdmin,
  IInstanceConfig,
  IInstanceConfiguration,
  IInstanceInfo,
  TFormattedInstanceConfiguration,
} from "@syncturtle/types";
import { ExternalStore } from "@syncturtle/utils";
import { CoreRootStore } from "./root.store";
import { InstanceService } from "@/services/instance.service";

type TError = {
  status: string;
  message: string;
};

export type TInstanceSnapshot = {
  isLoading: boolean;
  instance: IInstance | undefined;
  config: IInstanceConfig | undefined;
  instanceAdmins: IInstanceAdmin[] | undefined;
  instanceConfigurations: IInstanceConfiguration[] | undefined;
  error: TError | undefined;
  // version
  instanceConfigVersion: number;
};

const createInitialSnapshot = (): TInstanceSnapshot => ({
  isLoading: false,
  instance: undefined,
  config: undefined,
  instanceAdmins: undefined,
  instanceConfigurations: undefined,
  error: undefined,
  instanceConfigVersion: 0,
});

export interface IInstanceStore {
  // observables
  isLoading: boolean;
  instance: IInstance | undefined;
  config: IInstanceConfig | undefined;
  instanceAdmins: IInstanceAdmin[] | undefined;
  instanceConfigurations: IInstanceConfiguration[] | undefined;
  error: TError | undefined;
  // computed
  formattedConfig: TFormattedInstanceConfiguration | undefined;
  // action
  fetchInstanceInfo: () => Promise<IInstanceInfo | undefined>;
  updateInstanceInfo: (data: Partial<IInstance>) => Promise<IInstance | undefined>;
  fetchInstanceAdmins: () => Promise<IInstanceAdmin[] | undefined>;
  fetchInstanceConfigurations: () => Promise<IInstanceConfiguration[] | undefined>;
  updateInstanceConfigurations: (data: Partial<TFormattedInstanceConfiguration>) => Promise<IInstanceConfiguration[]>;
  disableEmail: () => Promise<void>;
}

export interface IInstanceStoreInternal extends IInstanceStore {
  _subscribe: ExternalStore<TInstanceSnapshot>["_subscribe"];
  _getSnapshot: ExternalStore<TInstanceSnapshot>["_getSnapshot"];
  _getServerSnapshot: ExternalStore<TInstanceSnapshot>["_getServerSnapshot"];
}

export class InstanceStore extends ExternalStore<TInstanceSnapshot> implements IInstanceStoreInternal {
  private readonly instanceService: InstanceService;

  constructor(private readonly _store: CoreRootStore) {
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

  get instanceAdmins(): IInstanceAdmin[] | undefined {
    return this.state.instanceAdmins;
  }

  get instanceConfigurations(): IInstanceConfiguration[] | undefined {
    return this.state.instanceConfigurations;
  }

  get error(): TError | undefined {
    return this.state.error;
  }

  // cached compute: formattedConfig
  private _cacheFormattedConfig?: {
    instanceConfigVersion: number;
    value: TFormattedInstanceConfiguration | undefined;
  };

  get formattedConfig(): TFormattedInstanceConfiguration | undefined {
    const { instanceConfigVersion, instanceConfigurations } = this.state;

    if (!instanceConfigurations) return;

    const cached = this._cacheFormattedConfig;
    if (cached && cached.instanceConfigVersion === instanceConfigVersion) {
      return cached.value;
    }

    // recompute
    const formatted = instanceConfigurations.reduce((formData: TFormattedInstanceConfiguration, config) => {
      formData[config.key] = config.value;
      return formData;
    }, {} as TFormattedInstanceConfiguration);

    this._cacheFormattedConfig = {
      instanceConfigVersion,
      value: formatted,
    };

    return formatted;
  }

  public fetchInstanceInfo = async (): Promise<IInstanceInfo> => {
    this.setState((s) => ({
      ...s,
      isLoading: s.instance === undefined ? true : s.isLoading,
      error: undefined,
    }));

    try {
      const instanceInfo = await this.instanceService.info();
      if (this.instance === undefined && !instanceInfo?.instance?.workspaceExist) {
        this._store.theme.toggleNewUserPopup();
      }

      this.setState({ isLoading: false, instance: instanceInfo.instance, config: instanceInfo.config });

      return instanceInfo;
    } catch (error) {
      this.setState({
        error: {
          status: "admin-instance-fetch-error",
          message: "Failed to fetch Instance info.",
        },
      });
      throw error;
    }
  };

  public updateInstanceInfo = async (data: Partial<IInstance>): Promise<IInstance | undefined> => {
    try {
      const instance = await this.instanceService.update(data);
      if (instance) {
        this.setState({ instance: instance });
      }
      return instance;
    } catch (error) {
      this.setState({
        error: {
          status: "update-instance-error",
          message: "Failed to update instance",
        },
      });
      throw error;
    }
  };

  public fetchInstanceAdmins = async (): Promise<IInstanceAdmin[] | undefined> => {
    try {
      const instanceAdmins = await this.instanceService.admins();
      if (instanceAdmins) {
        this.setState({ instanceAdmins: instanceAdmins });
      }
      return instanceAdmins;
    } catch (error) {
      console.log(error);
      throw error;
    }
  };

  public fetchInstanceConfigurations = async (): Promise<IInstanceConfiguration[]> => {
    try {
      const instanceConfigurations = await this.instanceService.configurations();
      if (instanceConfigurations) {
        this.setState({ instanceConfigurations: instanceConfigurations });
      }
      return instanceConfigurations;
    } catch (error) {
      console.log(error);
      throw error;
    }
  };

  public updateInstanceConfigurations = async (
    data: Partial<TFormattedInstanceConfiguration>
  ): Promise<IInstanceConfiguration[]> => {
    try {
      const response = await this.instanceService.updateConfigurations(data);
      const currentInstanceConfigurations = this.state.instanceConfigurations ?? [];
      const byKey = new Map(response.map((r) => [r.key, r]));

      const next = currentInstanceConfigurations.map((cfg) => byKey.get(cfg.key) ?? cfg);

      this.setState((s) => ({
        ...s,
        instanceConfigurations: next,
        instanceConfigVersion: s.instanceConfigVersion + 1,
      }));
      return response;
    } catch (error) {
      console.log(error);
      throw error;
    }
  };

  public disableEmail = async () => {
    const currentInstanceConfigurations = this.state.instanceConfigurations;

    if (!currentInstanceConfigurations?.length) return;

    const EMAIL_KEYS = new Set([
      "EMAIL_HOST",
      "EMAIL_PORT",
      "EMAIL_HOST_USER",
      "EMAIL_HOST_PASSWORD",
      "EMAIL_FROM",
      "ENABLE_SMTP",
    ]);

    // optimistic update
    this.setState((s) => ({
      ...s,
      instanceConfigurations: s.instanceConfigurations?.map((cfg) =>
        EMAIL_KEYS.has(cfg.key) ? { ...cfg, value: "" } : cfg
      ),
      instanceConfigVersion: s.instanceConfigVersion + 1,
      error: undefined,
    }));

    try {
      await this.instanceService.disableEmail();
    } catch (error) {
      this.setState((s) => ({
        ...s,
        instanceConfigurations: currentInstanceConfigurations,
        instanceConfigVersion: s.instanceConfigVersion + 1,
        error: {
          status: "disable-email-error",
          message: "Failed to disable email configuration",
        },
      }));
      throw error;
    }
  };
}
