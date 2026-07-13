export type TFileAssetPurpose = "USER_AVATAR" | "USER_COVER" | "WORKSPACE_LOGO";

// export enum EFileAssetPurpose {
//   USER_AVATAR = "USER_AVATAR",
//   USER_COVER = "USER_COVER",
//   WORKSPACE_LOGO = "WORKSPACE_LOGO",
// }

export type TFileMetaDataLite = {
  name: string;
  size: number;
  type: string;
};

export interface IPresignedPostData {
  url: string;
  fields: Record<string, string>;
}

export interface IFileUploadCreateRequest {
  purpose: TFileAssetPurpose;
  workspaceId?: string | null;
  originalFilename: string;
  contentType: string;
  sizeBytes: number;
}

export interface IFileUploadCreateResponse {
  assetId: string;
  assetUrl: string;
  uploadExpiresAt: string;
  uploadData: IPresignedPostData;
}

export interface IFileUploadResult {
  assetId: string;
  assetUrl: string;
  uploadExpiresAt: string;
}

export interface IWorkspaceLogoUpdateRequest {
  assetId: string;
}

export interface IUserAvatarUpdateRequest {
  assetId: string;
}

export interface IUserCoverUpdateRequest {
  assetId: string;
}
