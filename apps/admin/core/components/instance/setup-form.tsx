import type { FC } from "react";
import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
// heroui
import {
  Button,
  Checkbox,
  Description,
  FieldError,
  FieldGroup,
  Fieldset,
  Form,
  Input,
  InputGroup,
  Label,
  Spinner,
  TextField,
} from "@heroui/react";
import { Eye, EyeOff, Mail } from "lucide-react";
// components
import { AuthHeader } from "../common/auth-header";
import { Banner } from "../common/banner";
import { createSetupCsrfLoader, isSetupSubmitDisabled } from "./setup-csrf-loader";
// constants
import { API_BASE_URL, E_PASSWORD_STRENGTH } from "@syncturtle/constants";
// services
import { AuthService } from "@/services/auth.service";
// utils
import { getPasswordStrength } from "@syncturtle/utils";

type TFormData = {
  firstName: string;
  lastName: string;
  email: string;
  companyName: string;
  password: string;
  confirmPassword?: string;
  isTelemetryEnabled: boolean;
};

const defaultFormData: TFormData = {
  firstName: "",
  lastName: "",
  email: "",
  companyName: "",
  password: "",
  isTelemetryEnabled: true,
};

const authService = new AuthService();

enum EErrorCodes {
  INSTANCE_NOT_CONFIGURED = "INSTANCE_NOT_CONFIGURED",
  ADMIN_ALREADY_EXIST = "ADMIN_ALREADY_EXIST",
  REQUIRED_EMAIL_PASSWORD_FIRST_NAME = "REQUIRED_EMAIL_PASSWORD_FIRST_NAME",
  INVALID_EMAIL = "INVALID_EMAIL",
  INVALID_PASSWORD = "INVALID_PASSWORD",
  USER_ALREADY_EXISTS = "USER_ALREADY_EXISTS",
}

type TError = {
  type: EErrorCodes | undefined;
  message: string | undefined;
};

type TCsrfStatus = "loading" | "ready" | "error";

const parseBool = (value: string | null, defaultValue: boolean) => {
  if (value == null) return defaultValue;
  const v = value.toLowerCase();
  if (v === "true") return true;
  if (v === "false") return false;
  return defaultValue;
};

export const InstanceSetupFrom: FC = (props) => {
  const {} = props;
  // search params
  const searchParams = useSearchParams();
  const firstNameParam = searchParams.get("firstName") || undefined;
  const lastNameParam = searchParams.get("lastName") || undefined;
  const companyNameParam = searchParams.get("companyName") || undefined;
  const emailParam = searchParams.get("email") || undefined;
  const isTelemetryEnabledParam = parseBool(searchParams.get("isTelemetryEnabled"), true);
  const errorCode = searchParams.get("error_code") || undefined;
  const errorMessage = searchParams.get("error_message") || undefined;

  // state
  const [showPassword, setShowPassword] = useState({
    password: false,
    retypePassword: false,
  });
  const [formData, setFormData] = useState<TFormData>(() => ({
    ...defaultFormData,
    firstName: firstNameParam ?? "",
    lastName: lastNameParam ?? "",
    companyName: companyNameParam ?? "",
    email: emailParam ?? "",
    isTelemetryEnabled: isTelemetryEnabledParam,
  }));

  const [csrfToken, setCsrfToken] = useState<string | undefined>(undefined);
  const [csrfStatus, setCsrfStatus] = useState<TCsrfStatus>("loading");
  const [isPasswordInputFocused, setIsPasswordInputFocused] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRetryPasswordInputFocused, setIsRetryPasswordInputFocus] = useState(false);
  const [confirmTouched, setConfirmTouched] = useState(false);

  const loadCsrfToken = useMemo(() => createSetupCsrfLoader(() => authService.requestCSRFToken()), []);

  const handleShowPassword = (key: keyof typeof showPassword) =>
    setShowPassword((prev) => ({ ...prev, [key]: !prev[key] }));

  const handleFormChange = (key: keyof TFormData, value: string | boolean) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  const requestCsrfToken = useCallback(async () => {
    void loadCsrfToken()
      .then((token) => {
        setCsrfToken(token);
        setCsrfStatus("ready");
      })
      .catch(() => {
        setCsrfStatus("error");
      });
  }, [loadCsrfToken]);

  const retryCsrfToken = useCallback(() => {
    setCsrfStatus("loading");
    setCsrfToken(undefined);
    requestCsrfToken();
  }, [requestCsrfToken]);

  useEffect(() => {
    requestCsrfToken();
  }, [requestCsrfToken]);

  // derived values
  const errorData: TError = useMemo(() => {
    if (errorCode && errorMessage) {
      switch (errorMessage) {
        case EErrorCodes.INSTANCE_NOT_CONFIGURED:
          return { type: EErrorCodes.INSTANCE_NOT_CONFIGURED, message: errorMessage };
        case EErrorCodes.ADMIN_ALREADY_EXIST:
          return { type: EErrorCodes.ADMIN_ALREADY_EXIST, message: errorMessage };
        case EErrorCodes.REQUIRED_EMAIL_PASSWORD_FIRST_NAME:
          return { type: EErrorCodes.REQUIRED_EMAIL_PASSWORD_FIRST_NAME, message: errorMessage };
        case EErrorCodes.INVALID_EMAIL:
          return { type: EErrorCodes.INVALID_EMAIL, message: errorMessage };
        case EErrorCodes.INVALID_PASSWORD:
          return { type: EErrorCodes.INVALID_PASSWORD, message: errorMessage };
        case EErrorCodes.USER_ALREADY_EXISTS:
          return { type: EErrorCodes.USER_ALREADY_EXISTS, message: errorMessage };
        default:
          return { type: undefined, message: undefined };
      }
    } else return { type: undefined, message: undefined };
  }, [errorCode, errorMessage]);

  const isButtonDisabled = useMemo(() => {
    const hasValidFormData = Boolean(
      formData.firstName &&
      formData.lastName &&
      formData.email &&
      formData.password &&
      getPasswordStrength(formData.password) === E_PASSWORD_STRENGTH.STRENGTH_VALID &&
      formData.password === formData.confirmPassword
    );

    return isSetupSubmitDisabled({ csrfToken, hasValidFormData, isSubmitting });
  }, [csrfToken, isSubmitting, formData]);

  return (
    <>
      {isPasswordInputFocused} {isRetryPasswordInputFocused}
      <AuthHeader />
      <div className="mt-10 flex w-full grow flex-col items-center justify-center py-6">
        <div className="relative flex w-full max-w-90 flex-col">
          <Form
            method="post"
            action={`${API_BASE_URL}/api/instances/admins/sign-up`}
            onSubmit={() => setIsSubmitting(true)}
            onInvalid={() => setIsSubmitting(false)}
          >
            <Fieldset>
              <Fieldset.Legend>Setup your Syncturtle instance</Fieldset.Legend>
              <Description>Post setup you will be able to manage this Syncturtle instance</Description>
              {errorData.type &&
                errorData?.message &&
                ![EErrorCodes.INVALID_EMAIL, EErrorCodes.INVALID_PASSWORD].includes(errorData.type) && (
                  <Banner type="error" message={errorData?.message} />
                )}
              {csrfStatus === "error" && (
                <div className="flex flex-col items-center gap-2" role="alert">
                  <Banner type="error" message="Unable to prepare secure setup. Try again." />
                  <Button type="button" onPress={retryCsrfToken}>
                    Retry secure setup
                  </Button>
                </div>
              )}
              <FieldGroup>
                <Input type="hidden" name="csrfmiddlewaretoken" value={csrfToken ?? ""} />
                <Input type="hidden" name="isTelemetryEnabled" value={formData.isTelemetryEnabled ? "True" : "False"} />
                <div className="flex flex-col items-center gap-4 sm:flex-row">
                  <TextField
                    isRequired
                    name="firstName"
                    type="text"
                    value={formData.firstName}
                    onChange={(value: string) => handleFormChange("firstName", value)}
                  >
                    <Label>First name</Label>
                    <Input placeholder="Luna" />
                    <FieldError />
                  </TextField>
                  <TextField
                    isRequired
                    name="lastName"
                    type="text"
                    value={formData.lastName}
                    onChange={(value: string) => handleFormChange("lastName", value)}
                  >
                    <Label>Last name</Label>
                    <Input placeholder="Snow" />
                    <FieldError />
                  </TextField>
                </div>
                <TextField
                  isRequired
                  name="email"
                  type="email"
                  value={formData.email}
                  onChange={(value: string) => handleFormChange("email", value)}
                >
                  <Label>Email</Label>
                  <InputGroup>
                    <InputGroup.Prefix>
                      <Mail className="h-4 w-4" />
                    </InputGroup.Prefix>
                    <InputGroup.Input placeholder="lunasnow@marvel.com" />
                  </InputGroup>
                  <FieldError />
                </TextField>
                <TextField
                  isRequired
                  name="companyName"
                  type="text"
                  value={formData.companyName}
                  onChange={(value: string) => handleFormChange("companyName", value)}
                >
                  <Label>Company name</Label>
                  <Input placeholder="Marvel Inc" />
                  <FieldError />
                </TextField>
                <TextField
                  isRequired
                  name="password"
                  type={showPassword.password ? "text" : "password"}
                  value={formData.password}
                  validate={(value: string) => {
                    if (value.length >= 1 && value.length < 8) {
                      return "Password must be at least 8 characters or more";
                    }
                    return null;
                  }}
                  onChange={(value: string) => handleFormChange("password", value)}
                >
                  <Label>Set a password</Label>
                  <InputGroup>
                    <InputGroup.Input
                      placeholder="New password..."
                      onFocus={() => setIsPasswordInputFocused(true)}
                      onBlur={() => setIsPasswordInputFocused(false)}
                    />
                    <InputGroup.Suffix className="pr-0">
                      <Button
                        isIconOnly
                        aria-label={showPassword.password ? "Hide password" : "Show password"}
                        size="sm"
                        variant="ghost"
                        onPress={() => handleShowPassword("password")}
                        // prevents the button from stealing focus from input
                        onMouseDown={(e) => e.preventDefault()}
                      >
                        {showPassword.password ? <Eye /> : <EyeOff />}
                      </Button>
                    </InputGroup.Suffix>
                  </InputGroup>
                  <Description>hello</Description>
                  <FieldError />
                </TextField>
                <TextField
                  isRequired
                  name="confirmPassword"
                  type={showPassword.retypePassword ? "text" : "password"}
                  value={formData.confirmPassword ?? ""}
                  validate={(value: string) => {
                    if (!confirmTouched) return null;
                    if (!value) return null;
                    if (value != formData.password) {
                      return "Passwods don't match";
                    }
                    return null;
                  }}
                  onChange={(value: string) => handleFormChange("confirmPassword", value)}
                >
                  <Label>Confirm password</Label>
                  <InputGroup>
                    <InputGroup.Input
                      placeholder="Confirm password..."
                      onFocus={() => setIsRetryPasswordInputFocus(true)}
                      onBlur={() => setConfirmTouched(true)}
                      onBlurCapture={() => setIsRetryPasswordInputFocus(false)}
                    />
                    <InputGroup.Suffix className="pr-0">
                      <Button
                        isIconOnly
                        aria-label={showPassword.retypePassword ? "Hide confirm password" : "Show confirm password"}
                        size="sm"
                        variant="ghost"
                        onPress={() => handleShowPassword("retypePassword")}
                        // prevents the button from stealing focus from input
                        onMouseDown={(e) => e.preventDefault()}
                      >
                        {showPassword.retypePassword ? <Eye /> : <EyeOff />}
                      </Button>
                    </InputGroup.Suffix>
                  </InputGroup>
                  <FieldError />
                </TextField>
                <Checkbox
                  id="isTelemetryEnabled"
                  name="isTelemetryEnabled_ui"
                  isSelected={formData.isTelemetryEnabled}
                  onChange={(isSelected: boolean) => handleFormChange("isTelemetryEnabled", isSelected)}
                >
                  <Checkbox.Control>
                    <Checkbox.Indicator />
                  </Checkbox.Control>
                  <Checkbox.Content>
                    <Label>
                      Allow Syncturtle to anonymously collect usage events.{" "}
                      <a
                        tabIndex={-1}
                        href="#"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-sm font-medium text-blue-500 hover:text-blue-600 shrink-0"
                      >
                        See More
                      </a>
                    </Label>
                  </Checkbox.Content>
                </Checkbox>
              </FieldGroup>
              <Fieldset.Actions>
                <Button type="submit" isPending={isSubmitting} isDisabled={isButtonDisabled}>
                  {({ isPending }) => (
                    <>
                      {isPending ? <Spinner color="current" size="sm" /> : null}
                      {isPending ? "Submitting..." : "Continue"}
                    </>
                  )}
                </Button>
              </Fieldset.Actions>
            </Fieldset>
          </Form>
        </div>
      </div>
    </>
  );
};
