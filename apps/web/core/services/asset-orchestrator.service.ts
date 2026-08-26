import type { IFileUploadCreateResponse, IFileUploadResult, TFileAssetPurpose } from "@syncturtle/types";
import { FileUploadService } from "./file-upload.service";
import { FileService } from "./file.service";
import { UserService } from "./user.service";
import { WorkspaceService } from "./workspace.service";
import { generateFileUploadPayload, getFileMetadataForUpload } from "@/helpers/file.helper";
import { createIdempotencyKey } from "@/helpers/common.helper";
import { EFileAssetPurpose } from "@syncturtle/constants";

export class AssetAcceptanceError extends Error {
  readonly asset: IFileUploadResult;

  constructor(message: string, asset: IFileUploadResult, cause: unknown) {
    super(message, { cause });
    this.name = "AssetAcceptanceError";
    this.asset = asset;
  }
}

export class AssetOrchestratorService {
  private readonly fileService: FileService;
  private readonly fileUploadService: FileUploadService;
  private readonly workspaceService: WorkspaceService;
  private readonly userService: UserService;

  constructor() {
    this.fileService = new FileService();
    this.fileUploadService = new FileUploadService();
    this.workspaceService = new WorkspaceService();
    this.userService = new UserService();

    this.cancelUpload = this.cancelUpload.bind(this);
  }

  async uploadAssetOnly(
    purpose: TFileAssetPurpose,
    file: File,
    workspaceId?: string | null,
    onUploadProgress?: (progress: number) => void
  ): Promise<IFileUploadResult> {
    const signedUpload = await this.createAndUpload(purpose, file, workspaceId, onUploadProgress);

    return {
      assetId: signedUpload.assetId,
      assetUrl: signedUpload.assetUrl,
      uploadExpiresAt: signedUpload.uploadExpiresAt,
    };
  }

  async uploadWorkspaceLogo(
    workspaceId: string,
    workspaceSlug: string,
    file: File,
    onUploadProgress?: (progress: number) => void
  ): Promise<IFileUploadResult> {
    const signedUpload = await this.createAndUpload(
      EFileAssetPurpose.WORKSPACE_LOGO,
      file,
      workspaceId,
      onUploadProgress
    );

    const asset = {
      assetId: signedUpload.assetId,
      assetUrl: signedUpload.assetUrl,
      uploadExpiresAt: signedUpload.uploadExpiresAt,
    };

    try {
      await this.workspaceService.updateWorkspaceLogo(workspaceSlug, { assetId: asset.assetId });
      return asset;
    } catch (error) {
      throw new AssetAcceptanceError("Workspace logo upload completed but was not accepted.", asset, error);
    }
  }

  async uploadUserAvatar(file: File, onUploadProgress?: (progress: number) => void): Promise<IFileUploadResult> {
    return this.uploadAssetOnly(EFileAssetPurpose.USER_AVATAR, file, null, onUploadProgress);
  }

  async acceptUserAvatar(assetId: string): Promise<void> {
    await this.userService.updateAvatar({ assetId });
  }

  async uploadUserCoverImage(file: File, onUploadProgress?: (progress: number) => void): Promise<IFileUploadResult> {
    const signedUpload = await this.createAndUpload(EFileAssetPurpose.USER_COVER, file, null, onUploadProgress);

    const asset = {
      assetId: signedUpload.assetId,
      assetUrl: signedUpload.assetUrl,
      uploadExpiresAt: signedUpload.uploadExpiresAt,
    };

    try {
      await this.userService.updateCoverImage({ assetId: asset.assetId });
      return asset;
    } catch (error) {
      throw new AssetAcceptanceError("Cover image upload completed but was not accepted.", asset, error);
    }
  }

  async clearWorkspaceLogo(workspaceSlug: string): Promise<void> {
    await this.workspaceService.clearWorkspaceLogo(workspaceSlug);
  }

  async clearUserAvatar(): Promise<void> {
    await this.userService.clearAvatar();
  }

  async clearUserCoverImage(): Promise<void> {
    await this.userService.clearCoverImage();
  }

  async deleteAsset(assetId: string): Promise<void> {
    await this.fileService.deleteAsset(assetId);
  }

  cancelUpload(): void {
    this.fileUploadService.cancelUpload();
  }

  private async createAndUpload(
    purpose: TFileAssetPurpose,
    file: File,
    workspaceId?: string | null,
    onUploadProgress?: (progress: number) => void
  ): Promise<IFileUploadCreateResponse> {
    const metadata = await getFileMetadataForUpload(file);
    let signedUpload: IFileUploadCreateResponse | null = null;

    try {
      signedUpload = await this.fileService.createUpload(
        {
          purpose,
          workspaceId,
          originalFilename: metadata.name,
          contentType: metadata.type,
          sizeBytes: metadata.size,
        },
        createIdempotencyKey()
      );

      const payload = generateFileUploadPayload(signedUpload, file);
      await this.fileUploadService.uploadFile(signedUpload.uploadData, payload, onUploadProgress);
      await this.fileService.completeUpload(signedUpload.assetId);

      return signedUpload;
    } catch (error) {
      if (signedUpload) {
        await this.discardAfterFailedUpload(signedUpload.assetId);
      }
      throw error;
    }
  }

  private async discardAfterFailedUpload(assetId: string): Promise<void> {
    try {
      await this.fileService.deleteAsset(assetId);
    } catch {
      // The server-side expiry cleanup remains the final recovery path.
    }
  }
}
