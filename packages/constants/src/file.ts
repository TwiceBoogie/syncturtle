import type { TFileAssetPurpose } from "@syncturtle/types";

export const MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB

export const ACCEPTED_AVATAR_IMAGE_MIME_TYPES_FOR_REACT_DROPZONE = {
  "image/jpeg": [],
  "image/jpg": [],
  "image/png": [],
  "image/webp": [],
};
export const ACCEPTED_COVER_IMAGE_MIME_TYPES_FOR_REACT_DROPZONE = {
  "image/jpeg": [],
  "image/jpg": [],
  "image/png": [],
  "image/webp": [],
};

export const EFileAssetPurpose = {
  USER_AVATAR: "USER_AVATAR",
  USER_COVER: "USER_COVER",
  WORKSPACE_LOGO: "WORKSPACE_LOGO",
} as const satisfies Record<TFileAssetPurpose, TFileAssetPurpose>;
