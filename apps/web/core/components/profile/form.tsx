import type { FC, SyntheticEvent } from "react";
import { useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { ChevronDown, CircleUserRound, ImageIcon, InfoIcon } from "lucide-react";
// heroui
import { Button, FieldError, Form, Input, Label, Spinner, TextField, toast } from "@heroui/react";
// syncturtle imports
import { useTranslation } from "@syncturtle/i18n";
import type {
  TFieldErrors,
  IUser,
  TUserProfile,
  IFileUploadResult,
  TAssetSelection,
  IUserUpdateRequest,
  IAssetReference,
} from "@syncturtle/types";
import { getFileURL } from "@syncturtle/utils";
// store hooks
import { useUser, useUserProfile } from "@/hooks/store/user";
// components
import { DeactivateAccountModal } from "@/components/account/deactivate-account-modal";
import { UserImageUploadModal } from "@/components/core/modals/user-image-upload-modal";
// helpers
import { getFieldErrors, getPublicErrorMessage } from "@/helpers/error.helper";
import { AssetOrchestratorService } from "@/services/asset-orchestrator.service";

interface IProfileFormProps {
  user: IUser;
  profile: TUserProfile;
}

interface IUserProfileFormData {
  avatar: TAssetSelection;
  coverImageUrl: string;
  firstName: string;
  lastName: string;
  displayName: string;
  email: string;
  role: string;
  language: string;
  userTimezone: string;
}

const initialAvatarSelection = (user: IUser): TAssetSelection => {
  if (!user.avatarAssetId || !user.avatarUrl) return { kind: "empty" };
  return { kind: "persisted", asset: { assetId: user.avatarAssetId, assetUrl: user.avatarUrl } };
};

const createInitialForm = (user: IUser, profile: TUserProfile): IUserProfileFormData => ({
  avatar: initialAvatarSelection(user),
  coverImageUrl: user.coverImageUrl ?? "",
  firstName: user.firstName ?? "",
  lastName: user.lastName ?? "",
  displayName: user.displayName ?? "",
  email: user.email ?? "",
  role: profile.role ?? "Product / Project Manager",
  language: profile.language ?? "en",
  userTimezone: user.userTimezone ?? Intl.DateTimeFormat().resolvedOptions().timeZone,
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

const DEFAULT_COVER_IMAGE = "https://images.unsplash.com/photo-1506383796573-caf02b4a79ab";

const validateFirstName = (value: string) => {
  const firstName = value.trim();

  if (!firstName) return "Please enter first name";
  if (firstName.length > 24) return "First name must be within 24 characters";

  return null;
};

const validateLastName = (value: string) => {
  const lastName = value.trim();
  if (!lastName) return "Please enter last name";
  if (lastName.length > 24) return "Last name must be within 24 characters";

  return null;
};

const validateDisplayName = (value: string) => {
  const displayName = value.trim();

  if (!displayName) return "Display name is required.";
  if (value.includes("  ")) {
    return "Display name can't have two consecutive spaces.";
  }
  if (displayName.replace(/\s/g, "").length < 1) {
    return "Display name must be at least 1 character long.";
  }
  if (displayName.replace(/\s/g, "").length > 20) {
    return "Display name must be less than 20 characters long.";
  }

  return null;
};

export const ProfileForm: FC<IProfileFormProps> = (props) => {
  const { user, profile } = props;
  const { workspaceSlug } = useParams();
  // hooks
  const { t } = useTranslation();
  const { data: currentUser, updateCurrentUser } = useUser();
  const { updateUserProfile } = useUserProfile();
  // states
  const [formData, setFormData] = useState<IUserProfileFormData>(() => createInitialForm(user, profile));
  const [errors, setErrors] = useState<TFieldErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isImageUploadModalOpen, setIsImageUploadModalOpen] = useState(false);
  const [isDeactivateAccountModalOpen, setIsDeactivateAccountModalOpen] = useState(false);
  const [isDeactivateExpanded, setIsDeactivateExpanded] = useState(false);

  const handleFormChange = <K extends keyof IUserProfileFormData>(key: K, value: IUserProfileFormData[K]) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  const submitProfile = async (nextFormData: IUserProfileFormData = formData) => {
    if (isSubmitting) return;

    setIsSubmitting(true);
    setErrors({});

    const avatarAssetId = selectedAsset(nextFormData.avatar)?.assetId ?? null;

    const userPayload: IUserUpdateRequest = {
      firstName: nextFormData.firstName.trim(),
      lastName: nextFormData.lastName.trim(),
      displayName: nextFormData.displayName.trim(),
      avatarAssetId,
    };

    const profilePayload: Partial<TUserProfile> = {
      role: nextFormData.role,
    };

    const updatePromise = (async () => {
      await updateCurrentUser(userPayload);

      const asset = selectedAsset(nextFormData.avatar);
      handleFormChange("avatar", asset ? { kind: "persisted", asset } : { kind: "empty" });

      await updateUserProfile(profilePayload);
    })();

    toast.promise(updatePromise, {
      loading: "Updating...",
      success: "Profile updated successfully.",
      error: (error) =>
        getPublicErrorMessage(error, "There was some error in updating your profile. Please try again."),
    });

    try {
      await updatePromise;
    } catch (error) {
      setErrors(getFieldErrors(error));
    } finally {
      setIsSubmitting(false);
    }
  };

  const onSubmit = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (isSubmitDisabled) return;

    await submitProfile();
  };

  const handleProfilePictureDelete = async () => {
    const currentSelection = formData.avatar;
    if (currentSelection.kind === "empty" || currentSelection.kind === "removed") return;

    try {
      if (currentSelection.kind === "pending") {
        const orchestrator = new AssetOrchestratorService();
        await orchestrator.deleteAsset(currentSelection.asset.assetId);
      }

      const persisted = persistedAsset(currentSelection);
      handleFormChange("avatar", persisted ? { kind: "removed", previous: persisted } : { kind: "empty" });

      toast("Success!", {
        description: "Profile picture will be removed when you save.",
      });
    } catch (error) {
      toast("Error!", {
        actionProps: {
          children: "Remove",
          onPress: () => toast.clear(),
          variant: "danger",
        },
        description: getPublicErrorMessage(
          error,
          "There was some error in deleting your profile picture. Please try again."
        ),
      });
    } finally {
      setIsImageUploadModalOpen(false);
    }
  };

  const handleAvatarUploadSuccess = async (asset: IFileUploadResult) => {
    if (formData.avatar.kind === "pending" && formData.avatar.asset.assetId !== asset.assetId) {
      const orchestrator = new AssetOrchestratorService();
      try {
        await orchestrator.deleteAsset(formData.avatar.asset.assetId);
      } catch {
        // Server-side unlinked-asset cleanup remains the recovery path.
      }
    }

    const previous = persistedAsset(formData.avatar);
    const nextFormData = {
      ...formData,
      avatar: { kind: "pending", asset, previous } satisfies TAssetSelection,
    };

    setFormData(nextFormData);
    setIsImageUploadModalOpen(false);
  };

  // derived values
  const avatar = selectedAsset(formData.avatar);
  const avatarUrl = avatar?.assetUrl ?? "";
  const coverImageUrl = formData.coverImageUrl.trim() || DEFAULT_COVER_IMAGE;
  const fullName = `${formData.firstName} ${formData.lastName}`.trim();

  const isSubmitDisabled =
    isSubmitting ||
    Boolean(validateFirstName(formData.firstName)) ||
    Boolean(validateLastName(formData.lastName)) ||
    Boolean(validateDisplayName(formData.displayName));

  return (
    <>
      <DeactivateAccountModal
        isOpen={isDeactivateAccountModalOpen}
        onClose={() => setIsDeactivateAccountModalOpen(false)}
      />
      <UserImageUploadModal
        isOpen={isImageUploadModalOpen}
        onClose={() => setIsImageUploadModalOpen(false)}
        handleRemove={handleProfilePictureDelete}
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
        onSuccess={handleAvatarUploadSuccess}
        value={avatar}
      />
      <div className="mb-4 flex w-full items-center gap-2 rounded-md bg-custom-primary-100/10 p-2 text-custom-primary-200">
        <InfoIcon className="h-4 w-4 shrink-0" />
        <div className="flex-1 text-sm font-medium">{t("settings_moved_to_preferences")}</div>
        <Link href={`/${workspaceSlug}/settings/account/preferences`}>
          <Button size="sm" variant="secondary">
            {t("go_to_preferences")}
          </Button>
        </Link>
      </div>

      <Form className="w-full" validationErrors={errors} onSubmit={onSubmit}>
        <div className="flex w-full flex-col gap-6">
          <div className="relative h-44 w-full">
            <img
              src={getFileURL(coverImageUrl)}
              className="h-44 w-full rounded-lg object-cover"
              alt={currentUser?.firstName ?? "Cover Image"}
            />

            <div className="absolute -bottom-6 left-6 flex items-end justify-between">
              <Button
                type="button"
                aria-label={avatarUrl ? "Change profile image" : "Upload profile image"}
                onPress={() => setIsImageUploadModalOpen(true)}
                className={`h-16 w-16 rounded-lg bg-custom-background-90 p-0`}
              >
                {avatarUrl ? (
                  <img
                    src={getFileURL(avatarUrl)}
                    className="size-full rounded-lg object-cover"
                    alt={currentUser?.displayName ?? "Profile image"}
                  />
                ) : (
                  <div className="size-full rounded-md bg-custom-background-80 p-2">
                    <CircleUserRound className="size-full text-custom-text-200" />
                  </div>
                )}
              </Button>
            </div>

            <div className="absolute bottom-3 right-3 flex">
              <Button>Image picker popover</Button>
            </div>
          </div>

          <div className="items-center mt-6 flex justify-center">
            <div className="flex flex-col">
              <div className="flex items-center text-lg font-medium text-custom-text-200">
                <span>{fullName}</span>
              </div>
              <span className="text-sm tracking-tight text-custom-text-300">{formData.email}</span>
            </div>

            <Button type="button" size="sm" variant="secondary" onPress={() => setIsImageUploadModalOpen(true)}>
              <ImageIcon className="size-4" />
              {avatarUrl ? "Change image" : "Upload image"}
            </Button>
          </div>

          <div className="flex flex-col gap-2">
            <div className="grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2 xl:grid-cols-3">
              <TextField
                isRequired
                name="firstName"
                type="text"
                value={formData.firstName}
                onChange={(value: string) => handleFormChange("firstName", value)}
                validate={validateFirstName}
                variant="secondary"
              >
                <Label>{t("first_name")}</Label>
                <Input
                  placeholder="Enter your first name"
                  autoComplete="given-name"
                  maxLength={24}
                  variant="secondary"
                />
                <FieldError />
              </TextField>
              <TextField
                name="lastName"
                type="text"
                value={formData.lastName}
                onChange={(value: string) => handleFormChange("lastName", value)}
                validate={validateLastName}
                variant="secondary"
              >
                <Label>{t("last_name")}</Label>
                <Input
                  placeholder="Enter your last name"
                  autoComplete="family-name"
                  maxLength={24}
                  variant="secondary"
                />
                <FieldError />
              </TextField>

              <TextField
                isRequired
                name="displayName"
                type="text"
                value={formData.displayName}
                onChange={(value: string) => handleFormChange("displayName", value)}
                validate={validateDisplayName}
                variant="secondary"
              >
                <Label>{t("display_name")}</Label>
                <Input placeholder="Enter your display name" maxLength={24} variant="secondary" />
                <FieldError />
              </TextField>

              <TextField isRequired name="email" type="email" value={formData.email} isDisabled variant="secondary">
                <Label>{t("auth.common.email.label")}</Label>
                <Input
                  placeholder="Enter your email"
                  autoComplete="email"
                  className="cursor-not-allowed bg-custom-background-90!"
                  variant="secondary"
                />
                <FieldError />
              </TextField>
            </div>
          </div>

          <div className="flex flex-col gap-1">
            <div className="flex items-center justify-between pb-8 pt-6">
              <Button variant="primary" type="submit" isPending={isSubmitting} isDisabled={isSubmitDisabled}>
                {({ isPending }) => (
                  <>
                    {isPending ? <Spinner color="current" size="sm" /> : null}
                    {isPending ? t("saving") : t("save_changes")}
                  </>
                )}
              </Button>
            </div>
          </div>
        </div>
      </Form>

      <div className="w-full border-t border-custom-border-100">
        <button
          type="button"
          aria-expanded={isDeactivateExpanded}
          className="flex w-full items-center justify-between py-4"
          onClick={() => setIsDeactivateExpanded((prev) => !prev)}
        >
          <span className="text-lg font-medium tracking-tight">{t("deactivate_account")}</span>
          <ChevronDown className={`h-5 w-5 transition-all ${isDeactivateExpanded ? "rotate-180" : ""}`} />
        </button>

        {isDeactivateExpanded && (
          <div className="flex flex-col gap-8 pb-4">
            <span className="text-sm tracking-tight">{t("deactivate_account_description")}</span>
            <div>
              <Button variant="danger" onPress={() => setIsDeactivateAccountModalOpen(true)}>
                {t("deactivate_account")}
              </Button>
            </div>
          </div>
        )}
      </div>
    </>
  );
};
