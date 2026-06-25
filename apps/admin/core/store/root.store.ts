import { InstanceStore } from "./instance.store";
import { ThemeStore } from "./theme.store";
import { UserStore } from "./user.store";
import { WorkspaceStore } from "./workspace.store";

export abstract class CoreRootStore {
  theme: ThemeStore;
  instance: InstanceStore;
  user: UserStore;
  workspace: WorkspaceStore;

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
