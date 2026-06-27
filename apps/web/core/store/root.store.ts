import { ThemeStore } from "@/store/theme.store";
import { RouterStore } from "./router.store";
import { InstanceStore } from "./instance.store";
import { UserStore } from "./user";
import { WorkspaceStore } from "./workspace";

export class CoreRootStore {
  workspace: WorkspaceStore;
  router: RouterStore;
  theme: ThemeStore;
  instance: InstanceStore;
  user: UserStore;

  constructor() {
    this.router = new RouterStore();
    this.theme = new ThemeStore();
    this.instance = new InstanceStore();
    this.user = new UserStore(this);
    this.workspace = new WorkspaceStore(this);
  }

  resetOnSignOut() {}
}
