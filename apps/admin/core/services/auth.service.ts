import { ADMIN_BASE_PATH, ADMIN_BASE_URL, API_BASE_URL } from "@syncturtle/constants";
import { APIService, HttpError } from "./api.service";
import type { IApiErrorPayload, ICsrfTokenData } from "@syncturtle/types";

export class AuthService extends APIService {
  private static logoutFrameSequence = 0;

  constructor() {
    super(API_BASE_URL);
  }

  async requestCSRFToken(): Promise<ICsrfTokenData> {
    try {
      return await this.requestCsrfToken();
    } catch (error) {
      const err = error as HttpError<IApiErrorPayload>;
      throw err.data ?? err;
    }
  }

  async signOut(): Promise<void> {
    const destination = AuthService.signedOutDestination();
    if (new URL(destination).origin !== window.location.origin) {
      throw new Error("Administrator logout destination must match the current app origin");
    }
    try {
      const token = await this.requestCsrfToken();
      if (token.scope !== "SESSION") throw new Error("Administrator logout requires SESSION CSRF");
      await this.submitSignOutForm(token.csrfToken, destination);
      this.announceAuthStateChanged();
    } finally {
      this.invalidateCsrfToken();
    }
  }

  static signedOutDestination(): string {
    const path = `${(ADMIN_BASE_PATH || "/god-mode").replace(/\/+$/, "")}/`;
    return new URL(path, ADMIN_BASE_URL || window.location.origin).href;
  }

  /** Preserve the native POST/303 contract without a CORS-dependent fetch of the landing page.
   * Only reaching the exact same-origin ADMIN entry confirms the server's success redirect.
   * No frame scripts run; errors/timeouts cannot publish or clear local authentication.
   */
  private submitSignOutForm(csrfToken: string, destination: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const frame = document.createElement("iframe");
      frame.name = `syncturtle-admin-logout-${++AuthService.logoutFrameSequence}`;
      frame.hidden = true;
      frame.title = "Administrator logout confirmation";
      frame.setAttribute("sandbox", "allow-forms allow-same-origin");
      const form = document.createElement("form");
      form.hidden = true;
      form.method = "POST";
      form.action = new URL("/auth/admin/sign-out", this.baseUrl || window.location.origin).href;
      form.target = frame.name;
      const input = document.createElement("input");
      input.type = "hidden";
      input.name = "csrfmiddlewaretoken";
      input.value = csrfToken;
      form.append(input);
      let finished = false;

      const finish = (confirmed: boolean) => {
        if (finished) return;
        finished = true;
        window.clearTimeout(timeout);
        frame.removeEventListener("load", loaded);
        window.removeEventListener("pagehide", pageHidden);
        input.value = "";
        form.remove();
        frame.remove();
        if (confirmed) resolve();
        else reject(new Error("Administrator logout was not confirmed"));
      };
      const loaded = () => {
        try {
          const location = frame.contentWindow?.location.href;
          // Insertion may emit an initial about:blank load before the POST finishes.
          if (location === "about:blank") return;
          const frameDocument = frame.contentDocument;
          finish(location === destination && frameDocument?.URL === destination && !!frameDocument.documentElement);
        } catch {
          // A cross-origin error page is unreadable. It is not a success signal.
          finish(false);
        }
      };
      const pageHidden = () => finish(false);
      frame.addEventListener("load", loaded);
      window.addEventListener("pagehide", pageHidden);
      const timeout = window.setTimeout(() => finish(false), 15_000);
      try {
        document.body.append(frame, form);
        // One native submission, no refresh and no second logout/CSRF retry loop.
        form.submit();
      } catch {
        finish(false);
      }
    });
  }
}
