import { InstanceStore, type TInstanceStore } from "./instance.store";
import { ThemeStore, type TThemeStore } from "./theme.store";
import { UserStore, type TUserStore } from "./user.store";
import { TWorkspaceStore, WorkspaceStore } from "./workspace.store";

export abstract class CoreRootStore {
  theme: TThemeStore;
  instance: TInstanceStore;
  user: TUserStore;
  workspace: TWorkspaceStore;

  constructor() {
    this.theme = new ThemeStore();
    this.instance = new InstanceStore(this);
    this.user = new UserStore(this);
    this.workspace = new WorkspaceStore(this);
  }

  resetOnSignOut() {
    this.user.reset();
  }
}
