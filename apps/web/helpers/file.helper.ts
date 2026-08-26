import { fileTypeFromBuffer } from "file-type";
import type { IFileUploadCreateResponse, TFileMetaDataLite } from "@syncturtle/types";

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
  const signatureType = await detectMimeTypeFromSignature(file);
  if (!signatureType) {
    throw new Error("The selected file does not have a supported image signature.");
  }

  const declaredType = file.type.trim().toLowerCase();
  const normalizedDeclaredType = declaredType === "image/jpg" ? "image/jpeg" : declaredType;

  if (normalizedDeclaredType && normalizedDeclaredType !== signatureType) {
    throw new Error("The selected file type and contents do not match.");
  }

  return signatureType;
};

export const getFileMetadataForUpload = async (file: File): Promise<TFileMetaDataLite> => {
  const fileType = await detectFileType(file);

  return {
    name: file.name,
    size: file.size,
    type: fileType,
  };
};
