import type { FC, SyntheticEvent } from "react";
import { useState } from "react";
import { Spinner } from "@bprogress/next";
import { ImageIcon } from "lucide-react";
// heroui
import { Button, FieldError, Form, Input, Label, TextField, toast } from "@heroui/react";
// syncturtle imports
import { getFileURL } from "@syncturtle/utils";
import type {
  TOnboardingStep,
  IUser,
  TFieldErrors,
  IFileUploadResult,
  TAssetSelection,
  IAssetReference,
  IUserUpdateRequest,
} from "@syncturtle/types";
// components
import { UserImageUploadModal } from "@/components/core/modals/user-image-upload-modal";
import { CommonOnboardingHeader } from "../common";
import { SetPasswordRoot } from "./set-password";
import { MarketingConsent } from "./consent";
// hooks
import { useUser, useUserProfile } from "@/hooks/store/user";
import { EOnboardingSteps } from "@syncturtle/constants";
// services
import { AuthService } from "@/services/auth.service";
import { getFieldErrors } from "@/helpers/error.helper";
import { AssetOrchestratorService } from "@/services/asset-orchestrator.service";

export interface IProfileSetupStepProps {
  handleStepChange: (step: TOnboardingStep, skipInvites?: boolean) => void;
}

interface IProfileSetupFormValues {
  firstName: string;
  lastName: string;
  avatar: TAssetSelection;
  password: string;
  confirmPassword: string;
  role?: string;
  useCase?: string;
  hasMarketingEmailConsent: boolean;
}

const initialAvatarSelection = (user: IUser | null | undefined): TAssetSelection => {
  if (!user?.avatarAssetId || !user.avatarUrl) return { kind: "empty" };
  return { kind: "persisted", asset: { assetId: user.avatarAssetId, assetUrl: user.avatarUrl } };
};

const createInitialForm = (user: IUser | null | undefined): IProfileSetupFormValues => ({
  firstName: user?.firstName ?? "",
  lastName: user?.lastName ?? "",
  avatar: initialAvatarSelection(user),
  password: "",
  confirmPassword: "",
  hasMarketingEmailConsent: true,
});

const selectedAsset = (selection: TAssetSelection): IAssetReference | null => {
  if (selection.kind === "persisted" || selection.kind === "pending") return selection.asset;
  if (selection.kind === "failed") return selection.asset ?? selection.previous;
  return null;
};

const persistedAsset = (selection: TAssetSelection): IAssetReference | null => {
  if (selection.kind === "persisted") return selection.asset;
  if (selection.kind === "pending" || selection.kind === "failed") return selection.previous;
  if (selection.kind === "removed") return selection.previous;
  return null;
};

const getAvatarFallback = (formData: IProfileSetupFormValues, user?: IUser) => {
  const value = formData.firstName.trim() || user?.displayName.trim() || user?.email.trim() || "S";
  return value.charAt(0).toUpperCase();
};

const validateName = (value: string) => {
  const name = value.trim();
  if (!name) return "First name is required";
  if (name.length > 24) return "First name must be within 24 characters";
  return null;
};

const validateLastName = (value: string) => {
  const lastName = value.trim();
  if (!lastName) return "Last name is required";
  if (lastName.length > 24) {
    return "Last name must be within 24 characters";
  }
  return null;
};

const validatePassword = (password: string, confirmPassword: string) => {
  const hasPasswordInput = password.length > 0 || confirmPassword.length > 0;
  if (!hasPasswordInput) return null;
  if (password.length < 8) {
    return "Password must be at least 8 characters or more";
  }
  return null;
};

const validateConfirmPassword = (password: string, confirmPassword: string) => {
  const hasPasswordInput = password.length > 0 || confirmPassword.length > 0;
  if (!hasPasswordInput) return null;
  if (password !== confirmPassword) {
    return "Passwords do not match";
  }
  return null;
};

const authService = new AuthService();

export const ProfileSetupStep: FC<IProfileSetupStepProps> = (props) => {
  const { handleStepChange } = props;
  // store hooks
  const { data: user, updateCurrentUser } = useUser();
  const { updateUserProfile } = useUserProfile();
  // states
  const [formData, setFormData] = useState<IProfileSetupFormValues>(() => createInitialForm(user));
  const [errors, setErrors] = useState<TFieldErrors>({});
  const [isImageUploadModalOpen, setIsImageUploadModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleFormChange = <K extends keyof IProfileSetupFormValues>(key: K, value: IProfileSetupFormValues[K]) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  const handleSetPassword = async (password: string) => await authService.setPassword({ password });

  const handleSubmitUserDetail = async (formData: IProfileSetupFormValues) => {
    const userDetailsPayload: IUserUpdateRequest = {
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      displayName: user?.displayName ?? formData.firstName.trim(),
      avatarAssetId: selectedAsset(formData.avatar)?.assetId ?? null,
    };
    await updateCurrentUser(userDetailsPayload);

    const asset = selectedAsset(formData.avatar);
    handleFormChange("avatar", asset ? { kind: "persisted", asset } : { kind: "empty" });

    if (formData.password) {
      await handleSetPassword(formData.password);
    }
  };

  const onSubmit = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!user || isSubmitting || isButtonDisabled) return;

    setIsSubmitting(true);

    try {
      await Promise.all([
        updateUserProfile({
          hasMarketingEmailConsent: formData.hasMarketingEmailConsent,
        }),
        handleSubmitUserDetail(formData),
      ]);
      handleStepChange(EOnboardingSteps.PROFILE_SETUP);
    } catch (error) {
      setErrors(getFieldErrors(error));
      toast("Error", {
        actionProps: {
          children: "Dismiss",
          onPress: () => toast.clear(),
          variant: "danger",
        },
        description: "User details update failed. Please try again!",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRemoveAvatar = async () => {
    const currentSelection = formData.avatar;
    if (currentSelection.kind === "pending") {
      await new AssetOrchestratorService().deleteAsset(currentSelection.asset.assetId);
    }

    const persisted = persistedAsset(currentSelection);
    handleFormChange("avatar", persisted ? { kind: "removed", previous: persisted } : { kind: "empty" });
  };

  // derived values
  const avatar = selectedAsset(formData.avatar);
  const avatarUrl = avatar?.assetUrl ?? "";
  const isPasswordAlreadySetup = !user?.isPasswordAutoset;
  const passwordError = validatePassword(formData.password, formData.confirmPassword);
  const confirmPasswordError = validateConfirmPassword(formData.password, formData.confirmPassword);

  const isButtonDisabled =
    !user ||
    isSubmitting ||
    Boolean(validateName(formData.firstName)) ||
    Boolean(validateLastName(formData.lastName)) ||
    (!isPasswordAlreadySetup && Boolean(passwordError || confirmPasswordError));

  return (
    <Form onSubmit={onSubmit} className="flex flex-col gap-10" validationErrors={errors}>
      <CommonOnboardingHeader title="Create your profile." description="This is how you will appear in Syncturtle." />
      <UserImageUploadModal
        isOpen={isImageUploadModalOpen}
        onClose={() => setIsImageUploadModalOpen(false)}
        handleRemove={handleRemoveAvatar}
        onFailure={(message) =>
          setFormData((previousForm) => ({
            ...previousForm,
            avatar: {
              kind: "failed",
              asset: null,
              previous: persistedAsset(previousForm.avatar),
              message,
            },
          }))
        }
        onSuccess={async (asset: IFileUploadResult) => {
          if (formData.avatar.kind === "pending" && formData.avatar.asset.assetId !== asset.assetId) {
            try {
              await new AssetOrchestratorService().deleteAsset(formData.avatar.asset.assetId);
            } catch {
              // Server-side unlinked-asset cleanup remains the recovery path.
            }
          }
          handleFormChange("avatar", { kind: "pending", asset, previous: persistedAsset(formData.avatar) });
          setIsImageUploadModalOpen(false);
        }}
        value={avatar}
      />
      <div className="flex items-center gap-4">
        <Button
          isIconOnly
          type="button"
          aria-label={avatarUrl ? "Change profile image" : "Upload profile image"}
          className=""
          onPress={() => setIsImageUploadModalOpen(true)}
        >
          {avatarUrl ? (
            <img
              src={getFileURL(avatarUrl)}
              alt={user?.displayName ?? "Profile image"}
              className="size-full rounded-full object-cover"
            />
          ) : (
            getAvatarFallback(formData, user)
          )}
        </Button>
        <Button type="button" size="sm" onPress={() => setIsImageUploadModalOpen(true)}>
          <ImageIcon className="size-4" />
          {avatarUrl ? "Change image" : "Upload image"}
        </Button>
      </div>

      <div className="flex flex-col w-full gap-6">
        <TextField
          isRequired
          name="firstName"
          type="text"
          value={formData.firstName}
          onChange={(value: string) => handleFormChange("firstName", value)}
          validate={validateName}
        >
          <Label>First name</Label>
          <Input autoFocus placeholder="Luna" autoComplete="given-name" variant="secondary" />
          <FieldError />
        </TextField>

        <TextField
          name="lastName"
          type="text"
          value={formData.lastName}
          onChange={(value: string) => handleFormChange("lastName", value)}
          validate={validateLastName}
        >
          <Label>Last name</Label>
          <Input placeholder="Snow" autoComplete="family-name" variant="secondary" />
          <FieldError />
        </TextField>

        {!isPasswordAlreadySetup && (
          <SetPasswordRoot
            onPasswordChange={(password: string) => handleFormChange("password", password)}
            onConfirmPasswordChange={(confirmPassword: string) => handleFormChange("confirmPassword", confirmPassword)}
          />
        )}
      </div>

      <Button type="submit" isPending={isSubmitting} isDisabled={isButtonDisabled} fullWidth>
        {({ isPending }) => (
          <>
            {isPending ? <Spinner color="current" /> : null}
            {isPending ? "Saving..." : "Continue"}
          </>
        )}
      </Button>
      <MarketingConsent
        isChecked={!!formData.hasMarketingEmailConsent}
        disabled={isSubmitting}
        handleChange={(value: boolean) => handleFormChange("hasMarketingEmailConsent", value)}
      />
    </Form>
  );
};
