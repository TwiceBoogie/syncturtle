"use client";

import { getPublicErrorMessage } from "@/helpers/error.helper";
import { useUser } from "@/hooks/store/user";
import { useAppRouter } from "@/hooks/use-app-router";
import { Button, Modal, toast } from "@heroui/react";
import { useTranslation } from "@syncturtle/i18n";
import { useState, type FC } from "react";

interface IDeactivateAccountModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const DeactivateAccountModal: FC<IDeactivateAccountModalProps> = (props) => {
  const { isOpen, onClose } = props;
  const router = useAppRouter();
  // hooks
  const { t } = useTranslation();
  const { deactivateAccount, signOut } = useUser();
  // states
  const [isDeactivating, setIsDeactivating] = useState(false);

  const handleClose = () => {
    if (isDeactivating) return;

    setIsDeactivating(false);
    onClose();
  };

  const handleDeleteAccount = async () => {
    if (isDeactivating) return;

    setIsDeactivating(true);

    try {
      await deactivateAccount();

      toast("Success!", {
        description: "Account deactivated successfully.",
      });

      signOut();
      router.push("/");
      onClose();
    } catch (error) {
      toast("Error!", {
        actionProps: {
          children: "Remove",
          onPress: () => toast.clear(),
          variant: "danger",
        },
        description: getPublicErrorMessage(error, "Could not deactivate account."),
      });
    } finally {
      setIsDeactivating(false);
    }
  };

  return (
    <Modal.Backdrop
      isOpen={isOpen}
      isDismissable={!isDeactivating}
      onOpenChange={(nextOpen) => {
        if (!nextOpen) handleClose();
      }}
    >
      <Modal.Container placement="center" size="lg" scroll="inside">
        <Modal.Dialog aria-label="Deactivate account">
          {({ close }) => (
            <>
              <Modal.Header>
                <Modal.Heading>{t("deactivate_your_account")}</Modal.Heading>
              </Modal.Header>
              <Modal.Body>
                <div>hello there</div>
              </Modal.Body>

              <Modal.Footer>
                <Button type="button" variant="danger" size="sm" onPress={() => handleDeleteAccount()}>
                  Deactivate account
                </Button>
                <Button type="button" size="sm" onPress={close}>
                  Close
                </Button>
              </Modal.Footer>
            </>
          )}
        </Modal.Dialog>
      </Modal.Container>
    </Modal.Backdrop>
  );
};
