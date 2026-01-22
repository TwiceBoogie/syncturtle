import { FC, useMemo, useState } from "react";
import { FormHeader } from "./form-header";
import { AuthHeader } from "./auth-header";
import { getPasswordStrength } from "@syncturtle/utils";
import { E_PASSWORD_STRENGTH } from "@syncturtle/constants";
import { Eye, EyeOff } from "lucide-react";

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

export const InstanceSetupFrom: FC = (props) => {
  const {} = props;
  // state
  const [showPassword, setShowPassword] = useState({
    password: false,
    retypePassword: false,
  });
  const [formData, setFormData] = useState<TFormData>(defaultFormData);
  const [csrfToken, setCsrfToken] = useState<string | undefined>(undefined);
  const [isPasswordInputFocused, setIsPasswordInputFocused] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRetryPasswordInputFocused, setIsRetryPasswordInputFocus] = useState(false);

  const handleShowPassword = (key: keyof typeof showPassword) =>
    setShowPassword((prev) => ({ ...prev, [key]: !prev[key] }));

  const handleFormChange = (key: keyof TFormData, value: string | boolean) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  const isButtonDisabled = useMemo(
    () =>
      !isSubmitting &&
      !formData.firstName &&
      !formData.lastName &&
      !formData.email &&
      !formData.password &&
      getPasswordStrength(formData.password) === E_PASSWORD_STRENGTH.STRENGTH_VALID &&
      formData.password === formData.confirmPassword
        ? false
        : true,
    [isSubmitting, formData.firstName, formData.lastName, formData.password, formData.confirmPassword, formData.email]
  );

  const password = formData?.password ?? "";
  const confirmPassword = formData?.confirmPassword ?? "";
  const renderPasswordMatchError = !isRetryPasswordInputFocused || confirmPassword.length >= password.length;

  return (
    <>
      {isPasswordInputFocused} {setCsrfToken}
      <AuthHeader />
      <div className="mt-10 flex w-full grow flex-col items-center justify-center py-6">
        <div className="relative flex w-full max-w-90 flex-col gap-6">
          <FormHeader
            heading="Setup your Syncturtle instance"
            subHeading="Post setup you will be able to manage this Syncturtle instance"
          />
          <form
            className="space-y-4"
            method="POST"
            onSubmit={() => setIsSubmitting(true)}
            onError={() => setIsSubmitting(false)}
          >
            <input type="hidden" name="csrfmiddlewaretoken" value={csrfToken} />
            <input type="hidden" name="isTelemetryEnabled" value={formData.isTelemetryEnabled ? "True" : "False"} />

            <div className="flex flex-col items-center gap-4 sm:flex-row">
              <div className="w-full space-y-1">
                <div className="flex items-center justify-between">
                  <label htmlFor="fistName" className="block text-sm/6 font-medium">
                    First name
                    <span className="ml-0.5 text-sm font-semibold text-red-500" aria-hidden>
                      *
                    </span>
                    <span className="sr-only">(required)</span>
                  </label>
                </div>
                <div className="mt-2">
                  <div className="flex items-center rounded-md bg-gray-800 px-3 outline-1 -outline-offset-1 outline-gray-600 has-[input:focus-within]:outline-2 has-[input:focus-within]:-outline-offset-2 has-[input:focus-within]:outline-indigo-500">
                    <input
                      id="firstName"
                      name="firstName"
                      type="text"
                      placeholder="Luna"
                      className="block min-w-0 grow py-1.5 text-base text-white placeholder:text-gray-500 focus:outline-none sm:text-sm/6"
                      value={formData.firstName}
                      onChange={(e) => handleFormChange("firstName", e.target.value)}
                      required
                    />
                  </div>
                </div>
              </div>
              <div className="w-full space-y-1">
                <div className="flex items-center justify-between">
                  <label htmlFor="lastName" className="block text-sm/6 font-medium">
                    Last name
                    <span className="ml-0.5 text-sm font-semibold text-red-500" aria-hidden>
                      *
                    </span>
                    <span className="sr-only">(required)</span>
                  </label>
                </div>
                <div className="mt-2">
                  <div className="flex items-center rounded-md bg-gray-800 px-3 outline-1 -outline-offset-1 outline-gray-600 has-[input:focus-within]:outline-2 has-[input:focus-within]:-outline-offset-2 has-[input:focus-within]:outline-indigo-500">
                    <input
                      id="lastName"
                      name="lastName"
                      type="text"
                      placeholder="Snow"
                      className="block min-w-0 grow py-1.5 text-base text-white placeholder:text-gray-500 focus:outline-none sm:text-sm/6"
                      value={formData.lastName}
                      onChange={(e) => handleFormChange("lastName", e.target.value)}
                      required
                    />
                  </div>
                </div>
              </div>
            </div>

            <div className="w-full space-y-1">
              <div className="flex items-center justify-between">
                <label htmlFor="email" className="block text-sm/6 font-medium">
                  Email
                  <span className="ml-0.5 text-sm font-semibold text-red-500" aria-hidden>
                    *
                  </span>
                  <span className="sr-only">(required)</span>
                </label>
              </div>
              <div className="mt-2">
                <div className="flex items-center rounded-md bg-gray-800 px-3 outline-1 -outline-offset-1 outline-gray-600 has-[input:focus-within]:outline-2 has-[input:focus-within]:-outline-offset-2 has-[input:focus-within]:outline-indigo-500">
                  <input
                    id="email"
                    name="email"
                    type="email"
                    placeholder="lunasnow@marvel.com"
                    className="block min-w-0 grow py-1.5 text-base text-white placeholder:text-gray-500 focus:outline-none sm:text-sm/6"
                    value={formData.email}
                    onChange={(e) => handleFormChange("email", e.target.value)}
                    required
                  />
                </div>
              </div>
            </div>
            <div className="w-full space-y-1">
              <div className="flex items-center justify-between">
                <label htmlFor="companyName" className="block text-sm/6 font-medium">
                  Company name
                  <span className="ml-0.5 text-sm font-semibold text-red-500" aria-hidden>
                    *
                  </span>
                  <span className="sr-only">(required)</span>
                </label>
              </div>
              <div className="mt-2">
                <div className="flex items-center rounded-md bg-gray-800 px-3 outline-1 -outline-offset-1 outline-gray-600 has-[input:focus-within]:outline-2 has-[input:focus-within]:-outline-offset-2 has-[input:focus-within]:outline-indigo-500">
                  <input
                    id="companyName"
                    name="companyName"
                    type="text"
                    placeholder="Marvel"
                    className="block min-w-0 grow py-1.5 text-base text-white placeholder:text-gray-500 focus:outline-none sm:text-sm/6"
                    value={formData.companyName}
                    onChange={(e) => handleFormChange("companyName", e.target.value)}
                    required
                  />
                </div>
              </div>
            </div>
            <div className="w-full space-y-1">
              <div className="flex items-center justify-between">
                <label htmlFor="password" className="block text-sm/6 font-medium">
                  Set a password
                  <span className="ml-0.5 text-sm font-semibold text-red-500" aria-hidden>
                    *
                  </span>
                  <span className="sr-only">(required)</span>
                </label>
              </div>
              <div className="mt-2">
                <div className="flex items-center rounded-md bg-gray-800 px-3 outline-1 -outline-offset-1 outline-gray-600 has-[input:focus-within]:outline-2 has-[input:focus-within]:-outline-offset-2 has-[input:focus-within]:outline-indigo-500">
                  <input
                    id="password"
                    name="password"
                    type={showPassword.password ? "text" : "password"}
                    placeholder="New password..."
                    className="block min-w-0 grow py-1.5 text-base text-white placeholder:text-gray-500 focus:outline-none sm:text-sm/6"
                    value={formData.password}
                    onChange={(e) => handleFormChange("password", e.target.value)}
                    onFocus={() => setIsPasswordInputFocused(true)}
                    onBlur={() => setIsPasswordInputFocused(false)}
                    required
                  />
                  {showPassword.password ? (
                    <button type="button" tabIndex={-1} onClick={() => handleShowPassword("password")}>
                      <EyeOff className="h-4 w-4" />
                    </button>
                  ) : (
                    <button type="button" tabIndex={-1} onClick={() => handleShowPassword("password")}>
                      <Eye className="h-4 w-4" />
                    </button>
                  )}
                </div>
              </div>
            </div>
            <div className="w-full space-y-1">
              <div className="flex items-center justify-between">
                <label htmlFor="confirmPassword" className="block text-sm/6 font-medium">
                  Confirm password
                  <span className="ml-0.5 text-sm font-semibold text-red-500" aria-hidden>
                    *
                  </span>
                  <span className="sr-only">(required)</span>
                </label>
              </div>
              <div className="mt-2">
                <div className="flex items-center rounded-md bg-gray-800 px-3 outline-1 -outline-offset-1 outline-gray-600 has-[input:focus-within]:outline-2 has-[input:focus-within]:-outline-offset-2 has-[input:focus-within]:outline-indigo-500">
                  <input
                    id="confirmPassword"
                    name="confirmPassword"
                    type={showPassword.retypePassword ? "text" : "password"}
                    placeholder="Confirm password"
                    className="block min-w-0 grow py-1.5 text-base text-white placeholder:text-gray-500 focus:outline-none sm:text-sm/6"
                    value={formData.confirmPassword}
                    onChange={(e) => handleFormChange("confirmPassword", e.target.value)}
                    onFocus={() => setIsRetryPasswordInputFocus(true)}
                    onBlur={() => setIsRetryPasswordInputFocus(false)}
                    required
                  />
                  {showPassword.retypePassword ? (
                    <button type="button" tabIndex={-1} onClick={() => handleShowPassword("retypePassword")}>
                      <EyeOff className="h-4 w-4" />
                    </button>
                  ) : (
                    <button type="button" tabIndex={-1} onClick={() => handleShowPassword("retypePassword")}>
                      <Eye className="h-4 w-4" />
                    </button>
                  )}
                </div>
                {!!formData.confirmPassword &&
                  formData.password !== formData.confirmPassword &&
                  renderPasswordMatchError && <span className="text-sm text-red-500">Passwords don&apos;t match</span>}
              </div>
            </div>

            <div className="relative flex gap-2">
              <div>checkbox</div>
            </div>
            <div className="py-2">
              <button type="submit" disabled={isButtonDisabled}>
                {isSubmitting ? "Loading..." : "Continue"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  );
};
