import type { FC, SyntheticEvent } from "react";
import { useState } from "react";
import { Spinner } from "@bprogress/next";
import { ImageIcon } from "lucide-react";
// heroui
import { Button, FieldError, Form, Input, Label, TextField, toast } from "@heroui/react";
// syncturtle imports
import { getAssetIdFromUrl, getFileURL } from "@syncturtle/utils";
import type { TOnboardingStep, IUser, TFieldErrors } from "@syncturtle/types";
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

export interface IProfileSetupStepProps {
  handleStepChange: (step: TOnboardingStep, skipInvites?: boolean) => void;
}

interface IProfileSetupFormValues {
  firstName: string;
  lastName: string;
  avatarUrl?: string | null;
  password: string;
  confirmPassword: string;
  role?: string;
  useCase?: string;
  hasMarketingEmailConsent: boolean;
}

const createInitialForm = (user: IUser | null | undefined): IProfileSetupFormValues => ({
  firstName: user?.firstName ?? "",
  lastName: user?.lastName ?? "",
  avatarUrl: user?.avatarUrl ?? "",
  password: "",
  confirmPassword: "",
  hasMarketingEmailConsent: true,
});

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
    const userDetailsPayload: Partial<IUser> = {
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      avatarAssetId: getAssetIdFromUrl(formData.avatarUrl ?? "") ?? undefined,
    };
    try {
      await Promise.all([
        updateCurrentUser(userDetailsPayload),
        formData.password && handleSetPassword(formData.password),
      ]);
      console.log(userDetailsPayload);
    } catch (error) {
      setErrors(getFieldErrors(error));

      toast("Error", {
        actionProps: {
          children: "Remove",
          onPress: () => toast.clear(),
          variant: "danger",
        },
        description: "User details update failed. Please try again!",
      });
    }
  };

  const onSubmit = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!user || isSubmitting || isButtonDisabled) return;

    setIsSubmitting(true);

    updateUserProfile({
      hasMarketingEmailConsent: formData.hasMarketingEmailConsent,
    });
    await handleSubmitUserDetail(formData).then(() => {
      handleStepChange(EOnboardingSteps.PROFILE_SETUP);
    });

    setIsSubmitting(false);
  };

  const handleRemoveAvatar = () => {
    handleFormChange("avatarUrl", "");
  };

  // derived values
  const avatarUrl = formData?.avatarUrl?.trim() ? formData.avatarUrl.trim() : "";
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
        handleRemove={async () => handleRemoveAvatar()}
        onSuccess={(url) => {
          handleFormChange("avatarUrl", url);
          setIsImageUploadModalOpen(false);
        }}
        value={avatarUrl ?? null}
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
