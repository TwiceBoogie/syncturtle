"use client";

import type { FC } from "react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { CircleCheck, XCircle } from "lucide-react";
// heroui
import { Button, Description, Input, InputGroup, Label, Spinner, TextField } from "@heroui/react";
// syncturtle imports
import { API_BASE_URL } from "@syncturtle/constants";
import { useTranslation } from "@syncturtle/i18n";
// hooks
import { useCountdown } from "@/hooks/use-countdown";
// services
import { AuthService } from "@/services/auth.service";
// helpers
import { EAuthModes } from "@/helpers/authentication.helper";

interface IAuthUniqueCodeForm {
  mode: EAuthModes;
  email: string;
  isExistingEmail: boolean;
  handleEmailClear: () => void;
  generateEmailUniqueCode: (email: string) => Promise<{ code: string } | undefined>;
  nextPath: string | undefined;
}

interface IUniqueCodeFormValues {
  email: string;
  code: string;
}

const defaultValues: IUniqueCodeFormValues = {
  email: "",
  code: "",
};

const RESEND_COOLDOWN_SECONDS = 5;

const authService = new AuthService();

export const AuthUniqueCodeForm: FC<IAuthUniqueCodeForm> = (props) => {
  const { mode, email, isExistingEmail, handleEmailClear, generateEmailUniqueCode, nextPath } = props;
  console.log(isExistingEmail);
  // states
  const [formData, setFormData] = useState<IUniqueCodeFormValues>({ ...defaultValues, email });
  const [csrfToken, setCsrfToken] = useState("");
  const [isRequestingNewCode, setIsRequestingNewCode] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  // hooks
  const { t } = useTranslation();
  const {
    seconds: resendCooldownSeconds,
    isRunning: isResendCooldownRunning,
    start: startResendCooldown,
    reset: resetResendCooldown,
  } = useCountdown(0);

  useEffect(() => {
    let ignore = false;

    authService
      .requestCSRFToken()
      .then((data) => {
        if (!ignore && data.csrfToken) {
          setCsrfToken(data.csrfToken);
        }
      })
      .catch(() => {
        console.error("Error while requesting CSRF token");
      });

    return () => {
      ignore = true;
    };
  }, []);

  const handleFormChange = (key: keyof IUniqueCodeFormValues, value: string) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  const requestNewCode = useCallback(async () => {
    if (!email || isRequestingNewCode || isResendCooldownRunning) return;

    try {
      setIsRequestingNewCode(true);

      const uniqueCode = await generateEmailUniqueCode(email);

      handleFormChange("code", uniqueCode?.code ?? "");
      startResendCooldown(RESEND_COOLDOWN_SECONDS);
    } catch (error) {
      resetResendCooldown();
      console.error(error);
    }
  }, [
    email,
    isRequestingNewCode,
    isResendCooldownRunning,
    generateEmailUniqueCode,
    startResendCooldown,
    resetResendCooldown,
  ]);
  // derived values
  const formAction = useMemo(() => {
    const authPath = mode === EAuthModes.SIGN_IN ? "magic-sign-in" : "magic-sign-up";

    return `${API_BASE_URL}/auth/${authPath}`;
  }, [mode]);
  const isRequestNewCodeDisabled = isRequestingNewCode || isResendCooldownRunning;
  const isVerifyDisabled = isRequestingNewCode || isSubmitting || !formData.code.trim();

  return (
    <form className="mt-5 space-y-4" action={formAction} onSubmit={() => setIsSubmitting(true)}>
      <input type="hidden" name="csrfmiddlewaretoken" value={csrfToken} />
      {nextPath && <input type="hidden" name="nextPath" value={nextPath} />}

      <TextField type="email" name="email" value={email} autoComplete="email" fullWidth variant="secondary" isReadOnly>
        <Label>{t("auth.common.email.label")}</Label>
        <InputGroup>
          <InputGroup.Input placeholder={t("auth.common.email.placeholder")} />
          {email.length > 0 && (
            <InputGroup.Suffix>
              <Button
                type="button"
                size="sm"
                variant="ghost"
                aria-label={t("aria_labels.auth_forms.clear_email")}
                onPress={handleEmailClear}
              >
                <XCircle className="size-5 stroke-custom-text-400" />
              </Button>
            </InputGroup.Suffix>
          )}
        </InputGroup>
      </TextField>

      <TextField
        type="text"
        name="code"
        value={formData.code}
        onChange={(value: string) => handleFormChange("code", value)}
        fullWidth
        variant="secondary"
        autoFocus
        autoComplete="one-time-code"
      >
        <Label>{t("auth.common.unique_code.label")}</Label>
        <Input placeholder={t("auth.common.unique_code.placeholder")} />
        <Description>
          <span className="flex items-center gap-1 font-medium text-green-700">
            <CircleCheck className="size-3" />
            {t("auth.common.unique_code.paste_code")}
          </span>
          <Button
            type="button"
            size="sm"
            variant="ghost"
            isDisabled={isRequestNewCodeDisabled}
            onPress={requestNewCode}
          >
            {resendCooldownSeconds > 0
              ? t("auth.common.resend_in", {
                  seconds: resendCooldownSeconds,
                })
              : isRequestingNewCode
                ? t("auth.common.unique_code.requesting_new_code")
                : t("common.resend")}
          </Button>
        </Description>
      </TextField>

      <Button type="submit" size="sm" fullWidth isDisabled={isVerifyDisabled} isPending={isSubmitting}>
        {({ isPending }) => (
          <>
            {isPending ? <Spinner color="current" /> : null}
            {isRequestingNewCode ? t("auth.common.unique_code.sending_code") : t("common.continue")}
          </>
        )}
      </Button>
    </form>
  );
};
