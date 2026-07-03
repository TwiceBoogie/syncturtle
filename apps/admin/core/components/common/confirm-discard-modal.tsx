import type { FC } from "react";
import Link from "next/link";
// heroui
import { Button, Modal } from "@heroui/react";

interface IConfirmDiscardModalProps {
  isOpen: boolean;
  onDiscardHref: string;
  handleClose: () => void;
}

export const ConfirmDiscardModal: FC<IConfirmDiscardModalProps> = (props) => {
  const { isOpen, onDiscardHref, handleClose } = props;

  return (
    <Modal isOpen={isOpen} onOpenChange={handleClose}>
      <Modal.Backdrop>
        <Modal.Container>
          <Modal.Dialog>
            <Modal.CloseTrigger />
            <Modal.Header>Discard changes?</Modal.Header>
            <Modal.Body>
              <p>You have unsaved authentication changes. If you leave this page, your changes will be lost.</p>
            </Modal.Body>
            <Modal.Footer>
              <Button onPress={handleClose}>Keep editing</Button>
              <Link href={onDiscardHref}>
                <Button variant="danger">Discard changes</Button>
              </Link>
            </Modal.Footer>
          </Modal.Dialog>
        </Modal.Container>
      </Modal.Backdrop>
    </Modal>
  );
};
