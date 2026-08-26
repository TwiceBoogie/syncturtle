"use client";

import type { FC } from "react";
import { useEffect, useMemo, useRef, useState } from "react";
import Link from "next/link";
// heroui
import { Alert, Button, CloseButton, FieldError, Input, InputGroup, Label, Meter, TextField } from "@heroui/react";
import { Eye, EyeOff, Info, XCircle } from "lucide-react";
// syncturtle imports
import { API_BASE_URL, E_PASSWORD_STRENGTH } from "@syncturtle/constants";
import { useTranslation } from "@syncturtle/i18n";
import { getPasswordStrength } from "@syncturtle/utils";
// helpers
import { EAuthModes, EAuthSteps } from "@/helpers/authentication.helper";
// services
import { AuthService } from "@/services/auth.service";
import { ForgotPasswordPopover } from "./forgot-password-popoer";

interface IAuthPasswordFormProps {
  email: string;
  isSMTPConfigured: boolean;
  mode: EAuthModes;
  handleEmailClear: () => void;
  handleAuthStep: (step: EAuthSteps) => void;
  nextPath: string | undefined;
}

interface IPasswordFormValues {
  email: string;
  password: string;
  confirmPassword?: string;
}

interface IShowPasswordState {
  password: boolean;
  retypePassword: boolean;
}

interface IPasswordStrengthFeedbackProps {
  password: string;
  isFocused: boolean;
}

const defaultValues: IPasswordFormValues = {
  email: "",
  password: "",
};

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
        valueLabel={`${value}%`}
      />
      <p className="text-xs text-onboarding-text-400">
        Use at least 8 characters with uppercase, lowercase, numbers, and a symbol.
      </p>
    </div>
  );
};

const authService = new AuthService();

export const AuthPasswordForm: FC<IAuthPasswordFormProps> = (props) => {
  const { email, isSMTPConfigured, mode, handleEmailClear, handleAuthStep, nextPath } = props;
  // store hooks
  const { t } = useTranslation();
  // ref
  const formRef = useRef<HTMLFormElement>(null);
  // states
  const [csrfToken, setCsrfToken] = useState<string | undefined>(undefined);
  const [passwordFormData, setPasswordFormData] = useState<IPasswordFormValues>({ ...defaultValues, email });
  const [showPassword, setShowPassword] = useState<IShowPasswordState>({
    password: false,
    retypePassword: false,
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isPasswordInputFocused, setIsPasswordInputFocused] = useState(false);
  const [isRetryPasswordInputFocused, setIsRetryPasswordInputFocused] = useState(false);
  const [isBannerMessage, setBannerMessage] = useState(false);
  // derived values
  const password = passwordFormData.password ?? "";
  const confirmPassword = passwordFormData.confirmPassword ?? "";
  const renderPasswordMatchError = !isRetryPasswordInputFocused || confirmPassword.length >= password.length;
  const isConfirmPasswordInvalid =
    mode === EAuthModes.SIGN_UP && !!confirmPassword && password !== confirmPassword && renderPasswordMatchError;

  const handleShowPassword = (key: keyof IShowPasswordState) => {
    setShowPassword((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const handleFormChange = (key: keyof IPasswordFormValues, value: string) => {
    setPasswordFormData((prev) => ({ ...prev, [key]: value }));
  };

  useEffect(() => {
    if (csrfToken !== undefined) return;
    authService.requestCSRFToken().then((data) => data?.csrfToken && setCsrfToken(data.csrfToken));
  }, [csrfToken]);

  const redirectToUniqueCodeSignIn = async () => {
    handleAuthStep(EAuthSteps.UNIQUE_CODE);
  };

  const isButtonDisabled = useMemo(() => {
    if (isSubmitting) return true;
    if (!password) return true;

    if (mode === EAuthModes.SIGN_UP) {
      return password !== confirmPassword;
    }

    return false;
  }, [confirmPassword, isSubmitting, mode, password]);

  const passwordSupport =
    mode === EAuthModes.SIGN_IN ? (
      <div className="w-full">
        {isSMTPConfigured ? (
          <Link
            href={`/accounts/forgot-password?email=${encodeURIComponent(email)}`}
            className="text-xs font-medium text-custom-primary-100"
          >
            {t("auth.common.forgot_password")}
          </Link>
        ) : (
          <ForgotPasswordPopover />
        )}
      </div>
    ) : (
      password.length > 0 &&
      getPasswordStrength(password) !== E_PASSWORD_STRENGTH.STRENGTH_VALID && (
        <PasswordStrengthFeedback password={password} isFocused={isPasswordInputFocused} />
      )
    );

  return (
    <>
      {isBannerMessage && mode === EAuthModes.SIGN_UP && (
        <Alert status="danger">
          <Alert.Indicator>
            <Info className="size-4 text-red-500" />
          </Alert.Indicator>
          <Alert.Content>
            <Alert.Description>{t("auth.sign_up.errors.password.strength")}</Alert.Description>
          </Alert.Content>

          <CloseButton aria-label={t("aria_labels.common.close")} onPress={() => setBannerMessage(false)} />
        </Alert>
      )}

      <form
        ref={formRef}
        className="mt-5 space-4-y"
        method="POST"
        action={`${API_BASE_URL}/auth/${mode === EAuthModes.SIGN_IN ? "sign-in" : "sign-up"}`}
        onSubmit={async (event) => {
          event.preventDefault();

          const isPasswordValid =
            mode === EAuthModes.SIGN_UP ? getPasswordStrength(password) === E_PASSWORD_STRENGTH.STRENGTH_VALID : true;

          if (!isPasswordValid) {
            setBannerMessage(true);
            return;
          }

          setIsSubmitting(true);

          formRef.current?.submit();
        }}
        onError={() => {
          setIsSubmitting(false);
        }}
      >
        <Input type="hidden" name="csrfmiddlewaretoken" value={csrfToken ?? ""} />
        <input type="hidden" value={passwordFormData.email} name="email" />

        {nextPath && <input type="hidden" value={nextPath} name="nextPath" />}

        <TextField
          fullWidth
          name="email-display"
          value={passwordFormData.email}
          onChange={(value) => handleFormChange("email", value)}
        >
          <Label className="text-sm font-medium text-onboarding-text-300">{t("auth.common.email.label")}</Label>

          <InputGroup fullWidth className={"border border-onboarding-border-100 bg-onboarding-background-200"}>
            <InputGroup.Input
              id="email"
              type="email"
              className={"disable-autofill-style placeholder:text-onboarding-text-400"}
              placeholder={t("auth.common.email.placeholder")}
            />
            {passwordFormData.email.length > 0 && (
              <InputGroup.Suffix>
                <Button
                  aria-label={t("aria_labels.auth_forms.clear_email")}
                  isIconOnly
                  size="sm"
                  type="button"
                  variant="ghost"
                  onPress={handleEmailClear}
                >
                  <XCircle className="size-5 stroke-custom-text-400" />
                </Button>
              </InputGroup.Suffix>
            )}
          </InputGroup>
        </TextField>

        <TextField fullWidth name="password" value={password} onChange={(value) => handleFormChange("password", value)}>
          <Label className="text-sm font-medium text-onboarding-text-300">
            {mode === EAuthModes.SIGN_IN ? t("auth.common.password.label") : t("auth.common.password.set_password")}
          </Label>

          <InputGroup fullWidth className={"border border-onboarding-border-100 bg-onboarding-background-200"}>
            <InputGroup.Input
              id="password"
              type={showPassword.password ? "text" : "password"}
              className={"disable-autofill-style placeholder:text-onboarding-text-400"}
              placeholder={t("auth.common.password.placeholder")}
              autoComplete="on"
              autoFocus
              onBlur={() => setIsPasswordInputFocused(false)}
              onFocus={() => setIsPasswordInputFocused(true)}
            />
            <InputGroup.Suffix>
              <Button
                aria-label={t(
                  showPassword.password
                    ? "aria_labels.auth_forms.hide_password"
                    : "aria_labels.auth_forms.show_password"
                )}
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

          {passwordSupport}
        </TextField>

        {mode == EAuthModes.SIGN_UP && (
          <TextField
            fullWidth
            isInvalid={isConfirmPasswordInvalid}
            name="confirmPassword"
            value={confirmPassword}
            onChange={(value) => handleFormChange("confirmPassword", value)}
          >
            <Label className="text-sm font-medium text-onboarding-text-300">
              {t("auth.common.password.confirm_password.label")}
            </Label>

            <InputGroup fullWidth className={"border border-onboarding-border-100 bg-onboarding-background-200"}>
              <InputGroup.Input
                id="confirm-password"
                type={showPassword.retypePassword ? "text" : "password"}
                className={"disable-autofill-style placeholder:text-onboarding-text-400"}
                placeholder={t("auth.common.password.confirm_password.placeholder")}
                onBlur={() => setIsRetryPasswordInputFocused(false)}
                onFocus={() => setIsRetryPasswordInputFocused(true)}
              />

              <InputGroup.Suffix>
                <Button
                  aria-label={t(
                    showPassword.retypePassword
                      ? "aria_labels.auth_forms.hide_password"
                      : "aria_labels.auth_forms.show_password"
                  )}
                  isIconOnly
                  size="sm"
                  type="button"
                  variant="ghost"
                  onPress={() => handleShowPassword("retypePassword")}
                >
                  {showPassword.retypePassword ? (
                    <EyeOff className="size-5 stroke-custom-text-400" />
                  ) : (
                    <Eye className="size-5 stroke-custom-text-400" />
                  )}
                </Button>
              </InputGroup.Suffix>
            </InputGroup>

            <FieldError className={"text-sm text-red-500"}>{t("auth.common.password.errors.match")}</FieldError>
          </TextField>
        )}

        <div className="space-y-2.5">
          {mode === EAuthModes.SIGN_IN ? (
            <>
              <Button
                fullWidth
                isDisabled={isButtonDisabled}
                isPending={isSubmitting}
                size="sm"
                type="submit"
                variant="primary"
              >
                {isSMTPConfigured ? t("common.continue") : t("common.go_to_workspace")}
              </Button>

              {isSMTPConfigured && (
                <Button fullWidth size="sm" type="button" variant="outline" onPress={redirectToUniqueCodeSignIn}>
                  {t("auth.common.sign_in_with_unique_code")}
                </Button>
              )}
            </>
          ) : (
            <Button
              fullWidth
              isDisabled={isButtonDisabled}
              isPending={isSubmitting}
              size="sm"
              type="submit"
              variant="primary"
            >
              Create account
            </Button>
          )}
        </div>
      </form>
    </>
  );
};
