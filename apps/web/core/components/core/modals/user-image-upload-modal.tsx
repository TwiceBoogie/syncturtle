"use client";

import type { FC } from "react";
import { useEffect, useMemo, useState } from "react";
// heroui
import { Button, Modal, Spinner, toast } from "@heroui/react";
import { useDropzone } from "react-dropzone";
import { UserCircle2 } from "lucide-react";
// utils
import { getFileURL } from "@syncturtle/utils";
// constants
import { ACCEPTED_AVATAR_IMAGE_MIME_TYPES_FOR_REACT_DROPZONE, MAX_FILE_SIZE } from "@syncturtle/constants";
import { AssetOrchestratorService } from "@/services/asset-orchestrator.service";
import type { IAssetReference, IFileUploadResult } from "@syncturtle/types";

interface IUserImageUploadModalProps {
  handleRemove: () => Promise<void>;
  isOpen: boolean;
  onClose: () => void;
  onFailure: (message: string) => void;
  onSuccess: (asset: IFileUploadResult) => Promise<void> | void;
  value: IAssetReference | null;
}

const assetOrchestrator = new AssetOrchestratorService();

export const UserImageUploadModal: FC<IUserImageUploadModalProps> = (props) => {
  const { handleRemove, isOpen, onClose, onFailure, onSuccess, value } = props;
  // states
  const [image, setImage] = useState<File | null>(null);
  const [isRemoving, setIsRemoving] = useState(false);
  const [isImageUploading, setIsImageUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const onDrop = (acceptedFiles: File[]) => {
    const acceptedFile = acceptedFiles[0];
    if (!acceptedFile) return;
    setUploadError(null);
    setImage(acceptedFile);
  };

  const { getRootProps, getInputProps, isDragActive, fileRejections } = useDropzone({
    onDrop,
    accept: ACCEPTED_AVATAR_IMAGE_MIME_TYPES_FOR_REACT_DROPZONE,
    maxSize: MAX_FILE_SIZE,
    multiple: false,
  });

  const handleClose = () => {
    assetOrchestrator.cancelUpload();
    setImage(null);
    setIsImageUploading(false);
    setUploadError(null);
    onClose();
  };

  const handleSubmit = async () => {
    if (!image || isImageUploading) return;

    setIsImageUploading(true);

    const promise = assetOrchestrator.uploadUserAvatar(image).then(async (data: IFileUploadResult) => {
      await onSuccess(data);
      setImage(null);
    });

    toast.promise(promise, {
      loading: "Uploading image...",
      success: "Image uploaded. Save the profile to apply it.",
      error: "Failed to upload image",
    });
    try {
      await promise;
    } catch (error) {
      const message = error instanceof Error ? error.message : "Failed to upload image.";
      setUploadError(message);
      onFailure(message);
    } finally {
      setIsImageUploading(false);
    }
  };

  const handleImageRemove = async () => {
    if (!value || isRemoving) return;

    setIsRemoving(true);

    const promise = handleRemove();
    toast.promise(promise, {
      loading: "Removing image...",
      success: "Image selection removed. Save the profile to apply it.",
      error: "Failed to remove image",
    });
    try {
      await promise;
    } catch (error) {
      console.log(error);
    } finally {
      setIsRemoving(false);
    }
  };

  const previewUrl = useMemo(() => {
    if (!image) return undefined;
    return URL.createObjectURL(image);
  }, [image]);

  useEffect(() => {
    if (!previewUrl) return;
    return () => URL.revokeObjectURL(previewUrl);
  }, [previewUrl]);

  const imageSrc = useMemo(() => {
    if (previewUrl) return previewUrl;
    if (value?.assetUrl.trim()) return getFileURL(value.assetUrl);

    return undefined;
  }, [previewUrl, value]);

  // derived values
  const fileRejectionErrorCode = fileRejections[0]?.errors[0]?.code;

  return (
    <Modal.Backdrop
      isOpen={isOpen}
      onOpenChange={(nextIsOpen) => {
        if (!nextIsOpen) handleClose();
      }}
    >
      <Modal.Container placement="center" size="lg" scroll="inside">
        <Modal.Dialog aria-label="Upload profile image">
          {({ close }) => (
            <>
              <Modal.Header>
                <Modal.Heading>Upload image</Modal.Heading>
              </Modal.Header>

              <Modal.Body>
                <div className="space-y-5">
                  <div className="flex items-center justify-center gap-3">
                    <div
                      {...getRootProps()}
                      className={[
                        "relative grid h-80 w-full place-items-center rounded-lg text-center outline-none",
                        "focus-visible:ring-2 focus-visible:ring-custom-primary focus-visible:ring-offset-2",
                        isImageUploading || isRemoving ? "cursor-not-allowed opacity-70" : "cursor-pointer",
                        imageSrc ? "" : "border-2 border-dashed border-custom-border-200 hover:bg-custom-background-90",
                        isDragActive ? "border-2 border-dashed border-custom-primary bg-custom-background-90" : "",
                      ].join(" ")}
                    >
                      {imageSrc ? (
                        <>
                          <Button
                            type="button"
                            size="sm"
                            variant="secondary"
                            isDisabled={isImageUploading || isRemoving}
                          >
                            Edit
                          </Button>
                          <img
                            src={imageSrc}
                            alt="Profile preview"
                            className="absolute left-0 top-0 size-full rounded-md object-cover"
                          />
                        </>
                      ) : (
                        <div>
                          <UserCircle2 className="mx-auto size-16 text-custom-text-200" />
                          <span className="mt-2 block text-sm font-medium text-custom-text-200">
                            {isDragActive ? "Drop image here to upload" : "Drag & drop image here"}
                          </span>
                        </div>
                      )}

                      <input {...getInputProps()} />
                    </div>
                  </div>
                  {fileRejectionErrorCode && (
                    <p className="text-sm text-red-500">
                      {fileRejectionErrorCode === "file-too-large"
                        ? "The image size cannot exceed 5 MB."
                        : "Please upload a file in a valid format."}
                    </p>
                  )}
                  {uploadError && <p className="text-sm text-red-500">{uploadError}</p>}
                  <p className="text-sm text-custom-text-200">File formats supported: .jpeg, .jpg, .png, .webp</p>
                </div>
              </Modal.Body>

              <Modal.Footer className="flex items-center justify-between">
                <Button
                  type="button"
                  variant="danger"
                  size="sm"
                  isPending={isRemoving}
                  isDisabled={!value || isImageUploading || isRemoving}
                  onPress={handleImageRemove}
                >
                  {({ isPending }) => (
                    <>
                      {isPending ? <Spinner color="current" /> : null}
                      {isPending ? "Removing..." : "Remove"}
                    </>
                  )}
                </Button>
                <div className="flex items-center gap-2">
                  <Button
                    type="button"
                    variant="secondary"
                    size="sm"
                    isDisabled={isImageUploading || isRemoving}
                    onPress={() => {
                      close();
                      handleClose();
                    }}
                  >
                    Cancel
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    isPending={isImageUploading}
                    isDisabled={!image || isRemoving || isImageUploading}
                    onPress={handleSubmit}
                  >
                    {({ isPending }) => (
                      <>
                        {isPending ? <Spinner color="current" /> : null}
                        {isPending ? "Uploading..." : "Upload & Save"}
                      </>
                    )}
                  </Button>
                </div>
              </Modal.Footer>
            </>
          )}
        </Modal.Dialog>
      </Modal.Container>
    </Modal.Backdrop>
  );
};
