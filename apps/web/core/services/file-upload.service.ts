import type { IPresignedPostData } from "@syncturtle/types";

export class FileUploadCancelledError extends Error {
  constructor() {
    super("File upload was cancelled.");
    this.name = "FileUploadCancelledError";
  }
}

export class FileUploadService {
  private xhr: XMLHttpRequest | null = null;

  uploadFile(
    uploadData: IPresignedPostData,
    formData: FormData,
    onUploadProgress?: (progress: number) => void
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      this.xhr = xhr;

      xhr.upload.onprogress = (event) => {
        if (!event.lengthComputable) {
          return;
        }

        const progress = Math.round((event.loaded / event.total) * 100);
        onUploadProgress?.(progress);
      };

      xhr.onload = () => {
        this.xhr = null;

        if (xhr.status >= 200 && xhr.status < 300) {
          resolve();
          return;
        }

        reject({
          status: xhr.status,
          message: xhr.responseText || "File upload failed.",
        });
      };

      xhr.onerror = () => {
        this.xhr = null;
        reject({
          status: xhr.status,
          message: xhr.responseText || "File upload failed.",
        });
      };

      xhr.onabort = () => {
        this.xhr = null;
        reject(new FileUploadCancelledError());
      };

      xhr.open("POST", uploadData.url);
      xhr.withCredentials = false;
      xhr.send(formData);
    });
  }

  cancelUpload(): void {
    this.xhr?.abort();
    this.xhr = null;
  }
}
