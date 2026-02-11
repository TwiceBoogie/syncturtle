import { FC, useState } from "react";
// helpers
import { EAuthModes, EAuthSteps } from "@/helpers/authentication.helper";
import { useRouter } from "@bprogress/next";
import { useSearchParams } from "next/navigation";
import { AuthHeader } from "./auth-header";
import { useInstance } from "@/hooks/store/use-instance";
import { AuthEmailForm } from "./email";
import { IEmailCheckData } from "@syncturtle/types";
import { AuthService } from "@/services/auth.service";
import { AuthUniqueCodeForm } from "./unique-code";

interface IAuthRoot {
  authMode: EAuthModes;
}

const authService = new AuthService();

export const AuthRoot: FC<IAuthRoot> = (props) => {
  const router = useRouter();
  const searchParams = useSearchParams();
  // query params
  const emailParam = searchParams.get("email");
  const invitationId = searchParams.get("invitationId");
  const workspaceSlug = searchParams.get("slug");
  const errorCode = searchParams.get("errorCode");
  const nextPath = searchParams.get("nextPath");
  // props
  const { authMode: currentAuthMode } = props;
  // states
  const [authMode, setAuthMode] = useState<EAuthModes>(() => currentAuthMode);
  const [authStep, setAuthStep] = useState<EAuthSteps>(EAuthSteps.EMAIL);
  const [email, setEmail] = useState(emailParam ? emailParam.toString() : "");
  const [isExistingEmail, setIsExistingEmail] = useState(false);
  // hooks
  const { config } = useInstance();

  // derived values
  const isSMTPConfigured = config?.isSmtpConfigured || false;

  const handleEmailVerification = async (data: IEmailCheckData) => {
    setEmail(data.email);
    await authService.emailCheck(data).then(async (response) => {
      if (response.existing) {
        if (currentAuthMode === EAuthModes.SIGN_UP) setAuthMode(EAuthModes.SIGN_IN);
        if (response.status === "MAGIC_CODE") {
          setAuthStep(EAuthSteps.UNIQUE_CODE);
          generateEmailUniqueCode(data.email);
        } else if (response.status === "CREDENTIAL") {
          setAuthStep(EAuthSteps.PASSWORD);
        }
      } else {
        if (currentAuthMode === EAuthModes.SIGN_IN) setAuthMode(EAuthModes.SIGN_UP);
        if (response.status === "MAGIC_CODE") {
          setAuthStep(EAuthSteps.UNIQUE_CODE);
          generateEmailUniqueCode(data.email);
        } else if (response.status === "CREDENTIAL") {
          setAuthStep(EAuthSteps.PASSWORD);
        }
      }
      setIsExistingEmail(response.existing);
    });
  };

  const handleEmailClear = () => {
    setAuthMode(currentAuthMode);
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
        {errorCode}
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
      </AuthHeader>
    </div>
  );
};
