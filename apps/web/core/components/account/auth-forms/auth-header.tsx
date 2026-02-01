import { EAuthModes, EAuthSteps } from "@/helpers/authentication.helper";
import { FC, ReactNode } from "react";

interface IAuthHeader {
  workspaceSlug: string | undefined;
  invitationId: string | undefined;
  invitationEmail: string | undefined;
  authMode: EAuthModes;
  currentAuthStep: EAuthSteps;
  children: ReactNode;
}

export const AuthHeader: FC<IAuthHeader> = (props) => {
  const { children } = props;

  return (
    <>
      <div>
        <h1>header</h1>
        <p>sub-header</p>
      </div>
      {children}
    </>
  );
};
