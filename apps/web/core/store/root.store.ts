import { ThemeStore, TThemeStore } from "@/hooks/store/theme.store";
import { RouterStore, TRouterStore } from "./router.store";

export class CoreRootStore {
  router: TRouterStore;
  theme: TThemeStore;

  constructor() {
    this.router = new RouterStore();
    this.theme = new ThemeStore();
  }

  resetOnSignOut() {}
}
