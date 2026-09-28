import { useId } from "react";
import { Button, Modal, Spinner } from "@heroui/react";

interface IRevokeSessionsModalProps {
  isOpen: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  isPending: boolean;
  onClose: () => void;
  onConfirm: () => void;
}

export const RevokeSessionsModal = ({
  isOpen,
  title,
  description,
  confirmLabel,
  isPending,
  onClose,
  onConfirm,
}: IRevokeSessionsModalProps) => {
  const titleId = useId();
  const descriptionId = useId();

  const onOpenChange = (open: boolean) => {
    if (!open && !isPending) onClose();
  };

  return (
    <Modal.Backdrop isOpen={isOpen} isDismissable={!isPending} onOpenChange={onOpenChange} variant="opaque">
      <Modal.Container placement="center" size="md">
        <Modal.Dialog aria-labelledby={titleId} aria-describedby={descriptionId}>
          <Modal.Header className="flex items-center justify-between">
            <Modal.Heading id={titleId}>{title}</Modal.Heading>
            {!isPending && <Modal.CloseTrigger />}
          </Modal.Header>
          <Modal.Body>
            <p id={descriptionId} className="text-sm text-custom-text-200">
              {description}
            </p>
          </Modal.Body>
          <Modal.Footer>
            <Button variant="outline" isDisabled={isPending} onPress={onClose}>
              Cancel
            </Button>
            <Button variant="danger" isDisabled={isPending} onPress={onConfirm}>
              {isPending ? <Spinner aria-label="Revoking sessions" color="current" size="sm" /> : null}
              {isPending ? "Revoking..." : confirmLabel}
            </Button>
          </Modal.Footer>
        </Modal.Dialog>
      </Modal.Container>
    </Modal.Backdrop>
  );
};
