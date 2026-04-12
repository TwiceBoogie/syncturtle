// import { InstanceService } from "@/services/instance.service";
import { Button, Description, FieldError, Input, Label, Modal, Spinner, TextField } from "@heroui/react";
import { FC, useEffect, useMemo, useState } from "react";

interface ISendTestEmailModalProps {
  isOpen: boolean;
  handleClose: () => void;
}

enum ESendEmailSteps {
  SEND_EMAIL = "SEND_EMAIL",
  SUCCESS = "SUCCESS",
  FAILED = "FAILED",
}

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

// const instanceService = new InstanceService();

export const SendTestEmailModal: FC<ISendTestEmailModalProps> = (props) => {
  const { isOpen, handleClose } = props;

  // states
  const [receiverEmail, setReceiverEmail] = useState("");
  const [sendEmailStep, setSendEmailStep] = useState<ESendEmailSteps>(ESendEmailSteps.SEND_EMAIL);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");

  const emailError = useMemo(() => {
    if (sendEmailStep !== ESendEmailSteps.SEND_EMAIL) return "";
    if (!receiverEmail.trim()) return "Receiver email is required.";
    if (!emailRegex.test(receiverEmail.trim())) return "Enter a valid email address.";
    return "";
  }, [receiverEmail, sendEmailStep]);

  // reset state
  const resetState = () => {
    setReceiverEmail("");
    setSendEmailStep(ESendEmailSteps.SEND_EMAIL);
    setIsSubmitting(false);
    setError("");
  };

  useEffect(() => {
    if (!isOpen) {
      resetState();
    }
  }, [isOpen]);

  const onOpenChange = (open: boolean) => {
    if (!open) {
      handleClose();
    }
  };

  const handleSendEmail = async () => {
    if (emailError || isSubmitting) return;

    setIsSubmitting(true);
    setError("");

    try {
      console.log(error);
    } catch (error: unknown) {
      console.log(error);
      setSendEmailStep(ESendEmailSteps.FAILED);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Modal.Backdrop isOpen={isOpen} onOpenChange={onOpenChange} variant="opaque">
      <Modal.Container placement="top" size="md" scroll="inside">
        <Modal.Dialog aria-label="Send test email">
          {() => (
            <>
              <Modal.Header className="flex items-center justify-between">
                <Modal.Heading>
                  {sendEmailStep === ESendEmailSteps.SEND_EMAIL
                    ? "Send test email"
                    : sendEmailStep === ESendEmailSteps.SUCCESS
                      ? "Email send"
                      : "Failed"}
                </Modal.Heading>
                <Modal.CloseTrigger />
              </Modal.Header>
              <Modal.Body className="space-y-4 p-1">
                {sendEmailStep === ESendEmailSteps.SEND_EMAIL && (
                  <TextField
                    isRequired
                    name="receiverEmail"
                    value={receiverEmail}
                    onChange={setReceiverEmail}
                    isInvalid={Boolean(emailError)}
                    fullWidth
                  >
                    <Label>Receiver email</Label>
                    <Input type="email" placeholder="Receiver email" autoFocus />
                    <Description>We&apos;ll send a test email using your current SMTP configuration.</Description>
                    <FieldError>{emailError}</FieldError>
                  </TextField>
                )}
                {sendEmailStep === ESendEmailSteps.SUCCESS && (
                  <div className="flex flex-col gap-y-4 text-sm">
                    <p>
                      We have sent the test email to {receiverEmail}. Please check your spam folder if you cannot find
                      it.
                    </p>
                    <p>If you still cannot find it, recheck your SMTP configuration and trigger a new test email</p>
                  </div>
                )}
              </Modal.Body>
              <Modal.Footer>
                <Button variant="outline" slot="close">
                  {sendEmailStep === ESendEmailSteps.SEND_EMAIL ? "Cancel" : "Close"}
                </Button>
                {sendEmailStep === ESendEmailSteps.SEND_EMAIL && (
                  <Button
                    variant="primary"
                    isPending={isSubmitting}
                    isDisabled={Boolean(emailError) || isSubmitting}
                    onPress={handleSendEmail}
                  >
                    {({ isPending }) => (
                      <>
                        {isPending ? <Spinner size="sm" color="current" /> : null}
                        {isPending ? "Sending email..." : "Send email"}
                      </>
                    )}
                  </Button>
                )}
              </Modal.Footer>
            </>
          )}
        </Modal.Dialog>
      </Modal.Container>
    </Modal.Backdrop>
  );
};
