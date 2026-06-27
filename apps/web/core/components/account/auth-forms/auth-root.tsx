import type { FC } from "react";
import { useState } from "react";
import { useSearchParams } from "next/navigation";
import type { IEmailCheckData } from "@syncturtle/types";
// hooks
import { useInstance } from "@/hooks/store/use-instance";
import { useAppRouter } from "@/hooks/use-app-router";
// components
import {
  AuthHeader,
  AuthBanner,
  AuthEmailForm,
  AuthPasswordForm,
  OAuthOptions,
  TermsAndConditions,
  AuthUniqueCodeForm,
} from "@/components/account";
// helpers
// import { getRedirectErrorCode } from "@/helpers/error.helper";
import { EAuthModes, EAuthSteps, EErrorAlertType } from "@/helpers/authentication.helper";
import type { TAuthErrorInfo } from "@/helpers/authentication.helper";
// services
import { AuthService } from "@/services/auth.service";

interface IAuthRoot {
  authMode: EAuthModes;
}

const authService = new AuthService();

export const AuthRoot: FC<IAuthRoot> = (props) => {
  const { authMode: currentAuthMode } = props;
  const router = useAppRouter();
  const searchParams = useSearchParams();
  // query params
  const emailParam = searchParams.get("email") || undefined;
  const invitationId = searchParams.get("invitationId");
  const workspaceSlug = searchParams.get("slug");
  // const errorCode = getRedirectErrorCode(searchParams);
  const nextPath = searchParams.get("nextPath");
  // states
  const [authModeOverride, setAuthModeOverride] = useState<EAuthModes>(() => currentAuthMode);
  const [authStep, setAuthStep] = useState<EAuthSteps>(EAuthSteps.EMAIL);
  const [errorInfo, setErrorInfo] = useState<TAuthErrorInfo | undefined>(undefined);
  const [email, setEmail] = useState(emailParam ? emailParam.toString() : "");
  const [isExistingEmail, setIsExistingEmail] = useState(false);
  // hooks
  const { config } = useInstance();
  // derived values
  const isSMTPConfigured = config?.isSmtpConfigured || false;
  const authMode = authModeOverride ?? currentAuthMode ?? EAuthModes.SIGN_IN;

  // useEffect(() => {
  //   if (errorCode && authMode) {
  //     const errorHandler = authErrorHandler(errorCode?.toString(), emailParam);
  //     if (errorHandler) {
  //       // password error handler
  //       if ([EAuthenticationErrorCodes.AUTHENTICATION_FAILED_SIGN_UP].includes(errorHandler.code)) {
  //         setAuthModeOverride(EAuthModes.SIGN_UP);
  //         setAuthStep(EAuthSteps.PASSWORD);
  //       }
  //       if ([EAuthenticationErrorCodes.AUTHENTICATION_FAILED_SIGN_IN].includes(errorHandler.code)) {
  //         setAuthModeOverride(EAuthModes.SIGN_IN);
  //         setAuthStep(EAuthSteps.PASSWORD);
  //       }
  //       // magic_code error handler
  //       if (
  //         [
  //           EAuthenticationErrorCodes.INVALID_MAGIC_CODE_SIGN_UP,
  //           EAuthenticationErrorCodes.INVALID_EMAIL_MAGIC_SIGN_UP,
  //           EAuthenticationErrorCodes.EXPIRED_MAGIC_CODE_SIGN_UP,
  //         ].includes(errorHandler.code)
  //       ) {
  //         setAuthModeOverride(EAuthModes.SIGN_UP);
  //         setAuthStep(EAuthSteps.UNIQUE_CODE);
  //       }
  //       if (
  //         [
  //           EAuthenticationErrorCodes.INVALID_MAGIC_CODE_SIGN_IN,
  //           EAuthenticationErrorCodes.INVALID_EMAIL_MAGIC_SIGN_IN,
  //           EAuthenticationErrorCodes.EXPIRED_MAGIC_CODE_SIGN_IN,
  //         ].includes(errorHandler.code)
  //       ) {
  //         setAuthModeOverride(EAuthModes.SIGN_IN);
  //         setAuthStep(EAuthSteps.UNIQUE_CODE);
  //       }

  //       setErrorInfo(errorHandler);
  //     }
  //   }
  // }, [errorCode, authMode, emailParam]);

  const handleEmailVerification = async (data: IEmailCheckData) => {
    setEmail(data.email);
    setErrorInfo(undefined);

    try {
      const response = await authService.emailCheck(data);

      if (response.existingUser && currentAuthMode == EAuthModes.SIGN_UP) {
        setAuthModeOverride(EAuthModes.SIGN_IN);
      }

      if (!response.existingUser && currentAuthMode === EAuthModes.SIGN_IN) {
        setAuthModeOverride(EAuthModes.SIGN_UP);
      }

      if (response.authenticationFlow === "MAGIC_CODE") {
        setAuthStep(EAuthSteps.UNIQUE_CODE);
        await generateEmailUniqueCode(data.email);
      }

      if (response.authenticationFlow === "CREDENTIAL") {
        setAuthStep(EAuthSteps.PASSWORD);
      }

      setIsExistingEmail(response.existingUser);
    } catch (error) {
      console.log(error);
      // const authError = authErrorHandler(getApiErrorCode(error), data.email);
      // if (authError?.type === EErrorAlertType.BANNER_ALERT) {
      //   setErrorInfo(authError);
      //   return;
      // }
    }
  };

  const handleEmailClear = () => {
    setAuthModeOverride(currentAuthMode);
    setEmail("");
    setAuthStep(EAuthSteps.EMAIL);
    router.push(currentAuthMode === EAuthModes.SIGN_IN ? "/" : "/sign-up");
  };

  const generateEmailUniqueCode = async (email: string): Promise<{ code: string } | undefined> => {
    if (!isSMTPConfigured) return;
    console.log(`hit -> Email: ${email}`);
    return;
  };

  return (
    <div className="relative flex flex-col space-y-6">
      <AuthHeader
        workspaceSlug={workspaceSlug?.toString() || undefined}
        invitationId={invitationId?.toString() || undefined}
        invitationEmail={email || undefined}
        authMode={authMode}
        currentAuthStep={authStep}
      >
        {errorInfo && errorInfo?.type === EErrorAlertType.BANNER_ALERT && (
          <AuthBanner bannerData={errorInfo} handleBannerData={(value) => setErrorInfo(value)} />
        )}
        {authStep === EAuthSteps.EMAIL && <AuthEmailForm defaultEmail={email} onSubmit={handleEmailVerification} />}
        {authStep === EAuthSteps.UNIQUE_CODE && (
          <AuthUniqueCodeForm
            mode={authMode}
            email={email}
            isExistingEmail={isExistingEmail}
            handleEmailClear={handleEmailClear}
            generateEmailUniqueCode={generateEmailUniqueCode}
            nextPath={nextPath || undefined}
          />
        )}
        {authStep === EAuthSteps.PASSWORD && (
          <AuthPasswordForm
            mode={authMode}
            isSMTPConfigured={isSMTPConfigured}
            email={email}
            handleEmailClear={handleEmailClear}
            handleAuthStep={(step: EAuthSteps) => {
              if (step === EAuthSteps.UNIQUE_CODE) generateEmailUniqueCode(email);
              setAuthStep(step);
            }}
            nextPath={nextPath || undefined}
          />
        )}
        <OAuthOptions isSignUp={authMode === EAuthModes.SIGN_UP} />
        <TermsAndConditions authType={authMode} />
      </AuthHeader>
    </div>
  );
};
