"use client";

import { useState } from "react";
import { Eye, EyeOff } from "lucide-react";
// heroui
import {
  Button,
  Description,
  FieldGroup,
  Fieldset,
  Form,
  InputGroup,
  Label,
  Meter,
  Spinner,
  TextField,
} from "@heroui/react";
// syncturtle imports
import { E_PASSWORD_STRENGTH } from "@syncturtle/constants";
import { useTranslation } from "@syncturtle/i18n";
import { getPasswordStrength } from "@syncturtle/utils";
// components
import { PageHead } from "@/components/core";
import { ProfileSettingContentWrapper } from "@/components/profile/profile-setting-content-wrapper";
// store hooks
import { useUser } from "@/hooks/store/user";

interface IChangePasswordFormValues {
  oldPassword: string;
  newPassword: string;
  confirmPassword: string;
}

const defaultValues: IChangePasswordFormValues = {
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
};

interface IShowPasswordState {
  password: boolean;
  confirmPassword: boolean;
  oldPassword: boolean;
}

interface IPasswordStrengthFeedbackProps {
  password: string;
  isFocused: boolean;
}

const getPasswordStrengthPercent = (password: string) => {
  let score = 0;

  if (password.length >= 8) score += 1;
  if (password.length >= 12) score += 1;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score += 1;
  if (/\d/.test(password)) score += 1;
  if (/[^A-Za-z0-9]/.test(password)) score += 1;

  return Math.min(score * 20, 100);
};

const getPasswordStrengthColor = (value: number) => {
  if (value >= 80) return "success";
  if (value >= 60) return "warning";
  return "danger";
};

const PasswordStrengthFeedback = (props: IPasswordStrengthFeedbackProps) => {
  const { password, isFocused } = props;

  if (!password || !isFocused) return null;

  const value = getPasswordStrengthPercent(password);
  const color = getPasswordStrengthColor(value);
  return (
    <div className="space-y-1.5 pt-1">
      <Meter
        aria-label="Password strength"
        className="w-full"
        color={color}
        maxValue={100}
        minValue={0}
        size="sm"
        value={value}
      >
        <Meter.Track>
          <Meter.Fill />
        </Meter.Track>
      </Meter>
      <p className="text-xs text-onboarding-text-400">
        Use at least 8 characters with uppercase, lowercase, numbers, and a symbol.
      </p>
    </div>
  );
};

export default function SecurityPage() {
  // hooks
  const { t } = useTranslation();
  const { data: currentUser } = useUser();
  // states
  const [formData, setFormData] = useState<IChangePasswordFormValues>(defaultValues);
  const [showPassword, setShowPassword] = useState<IShowPasswordState>({
    password: false,
    confirmPassword: false,
    oldPassword: false,
  });
  const [isPasswordInputFocused, setIsPasswordInputFocused] = useState(false);
  const [isRetryPasswordInputFocused, setIsRetryPasswordInputFocused] = useState(false);
  // const [wasSubmitted, setWasSubmitted] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  console.log(setIsPasswordInputFocused, isRetryPasswordInputFocused, setIsRetryPasswordInputFocused, setIsSubmitting);
  // derived values
  const oldPassword = formData.oldPassword;
  const password = formData.newPassword;
  const confirmPassword = formData.confirmPassword;

  const oldPasswordRequired = !currentUser?.isPasswordAutoset;
  // const isNewPasswordSameAsOldPassword = oldPassword !== "" && password !== "" && password === oldPassword;

  const passwordStrength = getPasswordStrength(password);

  const isButtonDisabled =
    isSubmitting ||
    passwordStrength !== E_PASSWORD_STRENGTH.STRENGTH_VALID ||
    (oldPasswordRequired && oldPassword.trim() === "") ||
    password.trim() === "" ||
    confirmPassword.trim() === "" ||
    password !== confirmPassword ||
    password === oldPassword;

  const handleFormChange = <K extends keyof IChangePasswordFormValues>(key: K, value: IChangePasswordFormValues[K]) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  const handleShowPassword = (key: keyof IShowPasswordState) =>
    setShowPassword((prev) => ({ ...prev, [key]: !prev[key] }));

  return (
    <>
      <PageHead title="Profile - Security" />
      <ProfileSettingContentWrapper>
        <Form aria-label={t("auth.common.password.change_password.label.default")} validationBehavior="aria">
          <Fieldset>
            <Fieldset.Legend>{t("auth.common.password.change_password.label.default")}</Fieldset.Legend>
            <Description>Change your password here</Description>
            <FieldGroup>
              {oldPasswordRequired && (
                <TextField
                  isRequired
                  name="oldPassword"
                  type={showPassword.oldPassword ? "text" : "password"}
                  value={formData.oldPassword}
                  onChange={(value) => handleFormChange("oldPassword", value)}
                  variant="secondary"
                >
                  <Label>{t("auth.common.password.current_password.label")}</Label>
                  <InputGroup>
                    <InputGroup.Input placeholder={t("old_password")} autoComplete="current-password" />
                    <InputGroup.Suffix>
                      <Button
                        isIconOnly
                        size="sm"
                        type="button"
                        variant="ghost"
                        onPress={() => handleShowPassword("oldPassword")}
                      >
                        {showPassword.oldPassword ? (
                          <EyeOff className="size-5 stroke-custom-text-400" />
                        ) : (
                          <Eye className="size-5 stroke-custom-text-400" />
                        )}
                      </Button>
                    </InputGroup.Suffix>
                  </InputGroup>
                </TextField>
              )}
              <TextField
                isRequired
                name="newPassword"
                type={showPassword.password ? "text" : "password"}
                value={formData.newPassword}
                onChange={(value) => handleFormChange("newPassword", value)}
                variant="secondary"
              >
                <Label>{t("auth.common.password.new_password.label")}</Label>
                <InputGroup>
                  <InputGroup.Input
                    placeholder={t("auth.common.password.new_password.placeholder")}
                    autoComplete="new-password"
                  />
                  <InputGroup.Suffix>
                    <Button
                      isIconOnly
                      size="sm"
                      type="button"
                      variant="ghost"
                      onPress={() => handleShowPassword("password")}
                    >
                      {showPassword.password ? (
                        <EyeOff className="size-5 stroke-custom-text-400" />
                      ) : (
                        <Eye className="size-5 stroke-custom-text-400" />
                      )}
                    </Button>
                  </InputGroup.Suffix>
                </InputGroup>
                {password.length > 0 && passwordStrength !== E_PASSWORD_STRENGTH.STRENGTH_VALID && (
                  <PasswordStrengthFeedback password={password} isFocused={isPasswordInputFocused} />
                )}
              </TextField>
              <TextField
                isRequired
                name="confirmPassword"
                type={showPassword.confirmPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(value) => handleFormChange("confirmPassword", value)}
                variant="secondary"
              >
                <Label>{t("auth.common.password.confirm_password.label")}</Label>
                <InputGroup>
                  <InputGroup.Input
                    placeholder={t("auth.common.password.confirm_password.placeholder")}
                    autoComplete="new-password"
                  />
                  <InputGroup.Suffix>
                    <Button
                      isIconOnly
                      size="sm"
                      type="button"
                      variant="ghost"
                      onPress={() => handleShowPassword("confirmPassword")}
                    >
                      {showPassword.confirmPassword ? (
                        <EyeOff className="size-5 stroke-custom-text-400" />
                      ) : (
                        <Eye className="size-5 stroke-custom-text-400" />
                      )}
                    </Button>
                  </InputGroup.Suffix>
                </InputGroup>
              </TextField>
            </FieldGroup>
            <Fieldset.Actions>
              <Button type="submit" size="sm" isPending={isSubmitting} isDisabled={isButtonDisabled}>
                {({ isPending }) => (
                  <>
                    {isPending ? <Spinner color="current" /> : null}
                    {isPending
                      ? t("auth.common.password.change_password.label.submitting")
                      : t("auth.common.password.change_password.label.default")}
                  </>
                )}
              </Button>
            </Fieldset.Actions>
          </Fieldset>
        </Form>
      </ProfileSettingContentWrapper>
    </>
  );
}
