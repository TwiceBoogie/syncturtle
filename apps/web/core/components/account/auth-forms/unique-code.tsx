import type { FC } from "react";
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

export const AuthUniqueCodeForm: FC<IAuthUniqueCodeForm> = (props) => {
  const { mode, email, isExistingEmail, handleEmailClear, generateEmailUniqueCode, nextPath } = props;
  handleEmailClear();
  generateEmailUniqueCode("hi");
  return (
    <div>
      {mode} {email} {isExistingEmail} {nextPath}
    </div>
  );
};
