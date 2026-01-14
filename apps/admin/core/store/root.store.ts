import { InstanceStore, type TInstanceStore } from "./instance.store";
import { ThemeStore, type TThemeStore } from "./theme.store";
import { UserStore, type TUserStore } from "./user.store";

export abstract class CoreRootStore {
  theme: TThemeStore;
  instance: TInstanceStore;
  user: TUserStore;

  constructor() {
    this.theme = new ThemeStore();
    this.instance = new InstanceStore(this);
    this.user = new UserStore(this);
  }

  resetOnSignOut() {
    this.user.reset();
  }
}
