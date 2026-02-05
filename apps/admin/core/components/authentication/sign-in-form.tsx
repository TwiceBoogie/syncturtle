import { useSearchParams } from "next/navigation";
import { AuthHeader } from "../common/auth-header";
import { useEffect, useMemo, useState } from "react";
import {
  Button,
  Description,
  FieldError,
  FieldGroup,
  Fieldset,
  Form,
  Input,
  InputGroup,
  Label,
  Spinner,
  Surface,
  TextField,
} from "@heroui/react";
import { Eye, EyeOff } from "lucide-react";
import { API_BASE_URL } from "@syncturtle/constants";
import { AuthService } from "@/services/auth.service";
import { Banner } from "../common/banner";

enum EErrorCodes {
  INSTANCE_NOT_CONFIGURED = "INSTANCE_NOT_CONFIGURED",
  REQUIRED_EMAIL_PASSWORD = "REQUIRED_EMAIL_PASSWORD",
  INVALID_EMAIL = "INVALID_EMAIL",
  USER_DOES_NOT_EXIST = "USER_DOES_NOT_EXIST",
  AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED",
  INVALID_CSRF_TOKEN = "INVALID_CSRF_TOKEN",
}

type TError = {
  type: EErrorCodes | undefined;
  message: string | undefined;
};

type TFormData = {
  email: string;
  password: string;
};

const defaultFormData: TFormData = {
  email: "",
  password: "",
};

const authService = new AuthService();

export const InstanceSignInForm = () => {
  // search params
  const searchParams = useSearchParams();
  const emailParam = searchParams.get("email") || undefined;
  const errorCode = searchParams.get("error_code") || undefined;
  const errorMessage = searchParams.get("error_message") || undefined;
  // state
  const [formData, setFormData] = useState<TFormData>(() => ({
    ...defaultFormData,
    email: emailParam ?? "",
  }));
  const [showPassword, setShowPassword] = useState(false);
  const [csrfToken, setCsrfToken] = useState<string | undefined>(undefined);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleFormChange = (key: keyof TFormData, value: string | boolean) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  useEffect(() => {
    if (csrfToken === undefined) {
      authService.requestCSRFToken().then((data) => data.csrfToken && setCsrfToken(data.csrfToken));
    }
  }, [csrfToken]);

  // derived values
  const errorData: TError = useMemo(() => {
    if (errorCode && errorMessage) {
      switch (errorMessage) {
        case EErrorCodes.INSTANCE_NOT_CONFIGURED:
          return { type: EErrorCodes.INSTANCE_NOT_CONFIGURED, message: errorMessage };
        case EErrorCodes.REQUIRED_EMAIL_PASSWORD:
          return { type: EErrorCodes.REQUIRED_EMAIL_PASSWORD, message: errorMessage };
        case EErrorCodes.INVALID_EMAIL:
          return { type: EErrorCodes.INVALID_EMAIL, message: errorMessage };
        case EErrorCodes.USER_DOES_NOT_EXIST:
          return { type: EErrorCodes.USER_DOES_NOT_EXIST, message: errorMessage };
        case EErrorCodes.AUTHENTICATION_FAILED:
          return { type: EErrorCodes.AUTHENTICATION_FAILED, message: errorMessage };
        case EErrorCodes.INVALID_CSRF_TOKEN:
          return { type: EErrorCodes.INVALID_CSRF_TOKEN, message: errorMessage };
        default:
          return { type: undefined, message: undefined };
      }
    } else return { type: undefined, message: undefined };
  }, [errorCode, errorMessage]);

  const isButtonDisabled = useMemo(
    () => (!isSubmitting && formData.email && formData.password ? false : true),
    [formData.email, formData.password, isSubmitting]
  );

  return (
    <>
      <AuthHeader />
      <div className="flex flex-col grow w-full items-center justify-center py-6 mt-10">
        <div className="flex items-center justify-center rounded-3xl bg-surface p-6">
          <Surface className="w-full min-w-95">
            <Form
              method="post"
              action={`${API_BASE_URL}/api/instances/admins/sign-in`}
              onSubmit={() => setIsSubmitting(true)}
              onInvalid={() => setIsSubmitting(false)}
            >
              <Fieldset className="w-full">
                <Fieldset.Legend>Manage your Syncturtle instance</Fieldset.Legend>
                <Description>Configure instance-wide settings to secure your instance</Description>
                {errorData.type && errorData.message && <Banner type="error" message={errorData.message} />}
                <FieldGroup>
                  <Input type="hidden" name="csrfmiddlewaretoken" value={csrfToken ?? ""} />
                  <TextField
                    isRequired
                    name="email"
                    type="email"
                    value={formData.email}
                    onChange={(value: string) => handleFormChange("email", value)}
                  >
                    <Label>Email</Label>
                    <Input placeholder="luna.snow@marvel.com" variant="secondary" />
                    <FieldError />
                  </TextField>
                  <TextField
                    isRequired
                    name="password"
                    type={showPassword ? "text" : "password"}
                    value={formData.password}
                    validate={(value: string) => {
                      if (value.length >= 1 && value.length < 8) {
                        return "Password must be at least 8 characters or more";
                      }
                      return null;
                    }}
                    onChange={(value: string) => handleFormChange("password", value)}
                  >
                    <Label>Password</Label>
                    <InputGroup variant="secondary">
                      <InputGroup.Input placeholder="Enter your password" />
                      <InputGroup.Suffix>
                        <Button
                          isIconOnly
                          aria-label={showPassword ? "Hide password" : "Show password"}
                          size="sm"
                          variant="ghost"
                          onPress={() => setShowPassword(!showPassword)}
                          // prevents the button from stealing focus from input
                          onMouseDown={(e) => e.preventDefault()}
                        >
                          {showPassword ? <Eye /> : <EyeOff />}
                        </Button>
                      </InputGroup.Suffix>
                    </InputGroup>
                    <FieldError />
                  </TextField>
                </FieldGroup>
                <Fieldset.Actions>
                  <Button type="submit" isPending={isSubmitting} isDisabled={isButtonDisabled} fullWidth>
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
          </Surface>
        </div>
      </div>
    </>
  );
};
