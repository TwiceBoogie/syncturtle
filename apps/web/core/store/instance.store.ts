import { InstanceService } from "@/services/instance.service";
import { IInstance, IInstanceConfig, Listener, Unsubscribe } from "@syncturtle/types";
import { Emitter } from "@syncturtle/utils";

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

const initialSnapshot: TInstanceSnapshot = {
  isLoading: false,
  instance: undefined,
  config: undefined,
  error: undefined,
};

export interface IInstanceStoreInternal {
  _subscribe(listener: Listener): Unsubscribe;
  _getSnapshot(): TInstanceSnapshot;
  _getServerSnapshot(): TInstanceSnapshot;
  // observables
  isLoading: boolean;
  instance: IInstance | undefined;
  config: IInstanceConfig | undefined;
  error: TError | undefined;
  // actions
  fetchInstanceInfo: () => Promise<void>;
}

export type TInstanceStore = Omit<IInstanceStoreInternal, "_subscribe" | "_getSnapshot" | "_getServerSnapshot">;

export class InstanceStore implements IInstanceStoreInternal {
  private emitter = new Emitter();
  private _snap: TInstanceSnapshot = initialSnapshot;
  private instanceService = new InstanceService();

  // useSyncExternalStore integration
  /** @internal */
  public _subscribe = (listener: Listener): Unsubscribe => this.emitter.subscribe(listener);
  /** @internal */
  public _getSnapshot = (): TInstanceSnapshot => this._snap;
  /** @internal */
  public _getServerSnapshot = (): TInstanceSnapshot => this._snap;

  // raw getters for data
  get isLoading(): boolean {
    return this._snap.isLoading;
  }

  get instance(): IInstance | undefined {
    return this._snap.instance;
  }

  get config(): IInstanceConfig | undefined {
    return this._snap.config;
  }

  get error(): TError | undefined {
    return this._snap.error;
  }

  public fetchInstanceInfo = async (): Promise<void> => {
    this.set({ isLoading: true, error: undefined });

    try {
      const instanceInfo = await this.instanceService.getInstanceInfo();
      this.set({
        isLoading: false,
        instance: instanceInfo.instance,
        config: instanceInfo.config,
      });
    } catch (error) {
      this.set({
        isLoading: false,
        error: {
          status: "error",
          message: "Failed to fetch instance info.",
        },
      });
      throw error;
    }
  };

  private set(patch: Partial<TInstanceSnapshot>) {
    const prev = this._snap;
    const next = { ...prev, ...patch };

    let changed = false;
    for (const key in next) {
      const k = key as keyof TInstanceSnapshot;
      if (!Object.is(next[k], prev[k])) {
        changed = true;
        break;
      }
    }

    if (!changed) {
      return;
    }

    this._snap = next;
    this.emitter.emit();
  }
}
