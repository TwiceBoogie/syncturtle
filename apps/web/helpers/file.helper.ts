import { fileTypeFromBuffer } from "file-type";
import type { IFileUploadCreateResponse, TFileMetaDataLite } from "@syncturtle/types";
import { API_BASE_URL } from "@syncturtle/constants";

export const generateFileUploadPayload = (signedUpload: IFileUploadCreateResponse, file: File): FormData => {
  const formData = new FormData();

  Object.entries(signedUpload.uploadData.fields).forEach(([key, value]) => {
    formData.append(key, value);
  });

  formData.append("file", file);

  return formData;
};

const detectMimeTypeFromSignature = async (file: File): Promise<string> => {
  try {
    const chunk = file.slice(0, 4096);
    const buffer = await chunk.arrayBuffer();
    const bytes = new Uint8Array(buffer);

    const fileType = await fileTypeFromBuffer(bytes);

    return fileType?.mime || "";
  } catch {
    return "";
  }
};

const detectFileType = async (file: File): Promise<string> => {
  if (file.type && file.type.trim() !== "") {
    return file.type;
  }

  const signatureType = await detectMimeTypeFromSignature(file);
  if (signatureType) {
    return signatureType;
  }

  return "application/octet-stream";
};

export const getFileMetadataForUpload = async (file: File): Promise<TFileMetaDataLite> => {
  const fileType = await detectFileType(file);

  return {
    name: file.name,
    size: file.size,
    type: fileType,
  };
};

export const getAssetIdFromStaticUrl = (src: string): string => {
  const trimmed = src.trim().replace(/\/+$/, "");
  const parts = trimmed.split("/");

  return parts[parts.length - 1] ?? "";
};

export const getFileUrl = (path: string): string | undefined => {
  if (!path) return undefined;
  const isValidURL = path.startsWith("http");
  if (isValidURL) return path;
  return `${API_BASE_URL}${path}`;
};
