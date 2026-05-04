import { ThemeStore, TThemeStore } from "@/store/theme.store";
import { RouterStore, TRouterStore } from "./router.store";
import { InstanceStore, TInstanceStore } from "./instance.store";
import { TUserStore, UserStore } from "./user";

export class CoreRootStore {
  router: TRouterStore;
  theme: TThemeStore;
  instance: TInstanceStore;
  user: TUserStore;

  constructor() {
    this.router = new RouterStore();
    this.theme = new ThemeStore();
    this.instance = new InstanceStore();
    this.user = new UserStore(this);
  }

  resetOnSignOut() {}
}
