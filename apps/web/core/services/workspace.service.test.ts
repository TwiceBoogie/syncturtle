import { beforeEach, describe, expect, it, vi } from "vitest";
import { WorkspaceService } from "./workspace.service";

const mocks = vi.hoisted(() => ({
  get: vi.fn().mockResolvedValue({ data: { available: true } }),
  post: vi.fn(),
}));
vi.mock("./api.service", () => ({
  APIService: class {
    get = mocks.get;
    post = mocks.post;
  },
  HttpError: class {},
}));

beforeEach(() => vi.clearAllMocks());

describe("ordinary workspace slug check contract", () => {
  it("uses the workspace-owner route", async () => {
    const service = new WorkspaceService();
    await service.workspaceSlugCheck("personal-1234");
    expect(mocks.get).toHaveBeenCalledWith("/api/workspaces/slug-check", {
      params: { slug: "personal-1234" },
      signal: undefined,
    });
  });
});

describe("temporarily disconnected invitation contracts", () => {
  it("rejects single and bulk acceptance and detail lookup without making requests", async () => {
    const service = new WorkspaceService();
    await expect(service.joinWorkspace("family", "invite-id", { email: "someone@example.invalid" })).rejects.toThrow(
      "not available yet"
    );
    await expect(service.joinWorkspaces({ invitations: ["invite-id"] })).rejects.toThrow("not available yet");
    await expect(service.getWorkspaceInvitation("family", "invite-id")).rejects.toThrow("not available yet");
    expect(mocks.get).not.toHaveBeenCalled();
    expect(mocks.post).not.toHaveBeenCalled();
  });
});
