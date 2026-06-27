"use client";

import type { FC } from "react";
import { useState } from "react";
import { ArrowRightLeft } from "lucide-react";
// heroui
import { Button, Modal, Spinner } from "@heroui/react";
// hooks
import { useUser } from "@/hooks/store/user";
import { useAppRouter } from "@/hooks/use-app-router";

interface ISwitchAccountModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const SwitchAccountModal: FC<ISwitchAccountModalProps> = (props) => {
  const { isOpen, onClose } = props;
  // states
  const [switchingAccount, setSwitchingAccount] = useState(false);
  // router
  const router = useAppRouter();
  // hooks
  const { data: userData, signOut } = useUser();

  const handleClose = () => {
    setSwitchingAccount(false);
    onClose();
  };

  const handleSwitchAccount = async () => {
    setSwitchingAccount(true);

    try {
      await signOut();
      handleClose();
      router.push("/");
    } catch (error) {
      console.log("error: ", error);
    } finally {
      setSwitchingAccount(false);
    }
  };

  return (
    <Modal.Backdrop isOpen={isOpen} onOpenChange={handleClose}>
      <Modal.Container placement="center" size="lg">
        <Modal.Dialog aria-label="Switch account">
          <Modal.Body>
            <div className="flex gap-x-4">
              <div>
                <ArrowRightLeft className="size-5 text-custom-primary-100" aria-hidden />
              </div>
              <div>
                <Modal.Heading>Switch account</Modal.Heading>

                {userData?.email && (
                  <div className="text-base font-normal text-custom-text-200 mt-2">
                    If you have signed up via <span className="text-custom-primary-100">{userData.email}</span>{" "}
                    unintentionally, you can switch your account to a different one from here.
                  </div>
                )}
              </div>
            </div>
          </Modal.Body>
          <Modal.Footer>
            <Button
              variant="primary"
              isDisabled={switchingAccount}
              isPending={switchingAccount}
              onPress={handleSwitchAccount}
            >
              {({ isPending }) => (
                <>
                  {isPending ? <Spinner color="current" size="sm" /> : null}
                  {isPending ? "Switching..." : "Switch account"}
                </>
              )}
            </Button>
          </Modal.Footer>
        </Modal.Dialog>
      </Modal.Container>
    </Modal.Backdrop>
  );
};
