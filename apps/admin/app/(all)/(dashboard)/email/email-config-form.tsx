"use client";

import type { FC, SyntheticEvent, Key } from "react";
import { useMemo, useState } from "react";
import { useInstance } from "@/hooks/store/use-instance";
import { Button, Description, Form, Input, Label, ListBox, Select, Spinner, TextField, toast } from "@heroui/react";
import type { TFormattedInstanceConfiguration, TInstanceEmailConfigurationKeys } from "@syncturtle/types";
import { SendTestEmailModal } from "./test-email-modal";

interface IInstanceEmailForm {
  config: TFormattedInstanceConfiguration;
}

type EmailFormValues = Record<TInstanceEmailConfigurationKeys, string>;

type TEmailSecurityKeys = "EMAIL_USE_TLS" | "EMAIL_USE_SSL" | "NONE";
type TEditableEmailKeys =
  | "EMAIL_HOST"
  | "EMAIL_PORT"
  | "EMAIL_HOST_USER"
  | "EMAIL_HOST_PASSWORD"
  | "EMAIL_FROM"
  | "ENABLE_SMTP";

type TEmailFormState = Record<TEditableEmailKeys, string> & {
  emailSecurity: TEmailSecurityKeys;
};

const getInitialSecurity = (config: TFormattedInstanceConfiguration): TEmailSecurityKeys => {
  if (config["EMAIL_USE_TLS"] === "1") return "EMAIL_USE_TLS";
  if (config["EMAIL_USE_SSL"] === "1") return "EMAIL_USE_SSL";
  return "NONE";
};

const createInitialForm = (config: TFormattedInstanceConfiguration): TEmailFormState => ({
  EMAIL_HOST: config["EMAIL_HOST"] ?? "",
  EMAIL_PORT: config["EMAIL_PORT"] ?? "",
  EMAIL_HOST_USER: config["EMAIL_HOST_USER"] ?? "",
  EMAIL_HOST_PASSWORD: config["EMAIL_HOST_PASSWORD"] ?? "",
  EMAIL_FROM: config["EMAIL_FROM"] ?? "",
  ENABLE_SMTP: config["ENABLE_SMTP"] ?? "0",
  emailSecurity: getInitialSecurity(config),
});

const buildPayload = (form: TEmailFormState): EmailFormValues => ({
  EMAIL_HOST: form.EMAIL_HOST.trim(),
  EMAIL_PORT: form.EMAIL_PORT.trim(),
  EMAIL_HOST_USER: form.EMAIL_HOST_USER.trim(),
  EMAIL_HOST_PASSWORD: form.EMAIL_HOST_PASSWORD,
  EMAIL_FROM: form.EMAIL_FROM.trim(),
  ENABLE_SMTP: "1",
  EMAIL_USE_TLS: form.emailSecurity === "EMAIL_USE_TLS" ? "1" : "0",
  EMAIL_USE_SSL: form.emailSecurity === "EMAIL_USE_SSL" ? "1" : "0",
});

const EMAIL_SECURITY_OPTIONS: { [key in TEmailSecurityKeys]: string } = {
  EMAIL_USE_TLS: "TLS",
  EMAIL_USE_SSL: "SSL",
  NONE: "No email security",
};

export const InstanceEmailForm: FC<IInstanceEmailForm> = (props) => {
  const { config } = props;

  const initialForm = useMemo(() => createInitialForm(config), [config]);
  // states
  const [isSendTestEmailModalOpen, setIsSendTestEmailModalOpen] = useState(false);
  const [formData, setFormData] = useState<TEmailFormState>(initialForm);
  const [isSubmitting, setIsSubmitting] = useState(false);
  // store hooks
  const { updateInstanceConfigurations } = useInstance();

  const handleFormChange = (key: keyof TEmailFormState, value: string) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  const handleSubmit = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (isSubmitting) return;

    const payload: Partial<EmailFormValues> = buildPayload(formData);
    setIsSubmitting(true);

    try {
      console.log(payload);
      const promise = updateInstanceConfigurations(payload);

      toast.promise(promise, {
        loading: "Saving email settings...",
        success: "Email settings updated successfully",
        error: (err) => {
          if (err instanceof Error) return err.message;

          if (typeof err === "object" && err !== null && "message" in err) {
            const message = (err as { message?: unknown }).message;
            if (typeof message === "string" && message.trim()) {
              return message;
            }
          }
          return "Failed to update email settings";
        },
      });

      await promise;
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-8">
      <SendTestEmailModal isOpen={isSendTestEmailModalOpen} handleClose={() => setIsSendTestEmailModalOpen(false)} />
      <Form onSubmit={handleSubmit}>
        <div className="grid-col grid w-full max-w-4xl grid-cols-1 items-start justify-between gap-10 lg:grid-cols-2">
          <TextField
            isRequired
            name="EMAIL_HOST"
            type="text"
            value={formData.EMAIL_HOST}
            onChange={(value: string) => handleFormChange("EMAIL_HOST", value)}
          >
            <Label>Host</Label>
            <Input placeholder="email.google.com" />
          </TextField>
          <TextField
            isRequired
            name="EMAIL_PORT"
            type="text"
            value={formData.EMAIL_PORT}
            onChange={(value: string) => handleFormChange("EMAIL_PORT", value)}
          >
            <Label>Port</Label>
            <Input placeholder="8080" />
          </TextField>
          <TextField
            isRequired
            name="EMAIL_FROM"
            type="text"
            value={formData.EMAIL_FROM}
            onChange={(value: string) => handleFormChange("EMAIL_FROM", value)}
          >
            <Label>Sender&apos;s email address</Label>
            <Input placeholder="no-reply@projectsyncturtle.com" />
            <Description>
              This is the email address your users will see when getting emails from this instance. You will need to
              verify this address.
            </Description>
          </TextField>
          <Select
            isRequired
            value={formData.emailSecurity}
            onChange={(value: Key | null) =>
              handleFormChange("emailSecurity", value === null ? "" : (value as TEmailSecurityKeys))
            }
          >
            <Label>Email Security</Label>
            <Select.Trigger>
              <Select.Value />
              <Select.Indicator />
            </Select.Trigger>
            <Select.Popover>
              <ListBox>
                {Object.entries(EMAIL_SECURITY_OPTIONS).map(([key, value]) => (
                  <ListBox.Item key={key} id={key} textValue={value}>
                    {value}
                  </ListBox.Item>
                ))}
              </ListBox>
            </Select.Popover>
          </Select>
        </div>
        <div className="flex flex-col gap-6 my-6 pt-4 border-t border-custom-border-100">
          <div className="flex w-full max-w-xl flex-col gap-y-10 px-1">
            <div className="mr-8 flex items-center gap-10 pt-4">
              <div className="grow">
                <div className="text-sm font-medium text-custom-text-100">Authentication</div>
                <div className="text-xs font-normal text-custom-text-300">
                  This is optional, but we recommend setting up a username and a password for your SMTP server.
                </div>
              </div>
            </div>
          </div>
          <div className="grid-col grid w-full max-w-4xl grid-cols-1 items-center justify-between gap-10 lg:grid-cols-2">
            <TextField
              name="EMAIL_HOST_USER"
              type="text"
              value={formData.EMAIL_HOST_USER}
              onChange={(value: string) => handleFormChange("EMAIL_HOST_USER", value)}
            >
              <Label>Username</Label>
              <Input placeholder="example@test.com" />
            </TextField>
            <TextField
              name="EMAIL_HOST_PASSWORD"
              type="password"
              value={formData.EMAIL_HOST_PASSWORD}
              onChange={(value: string) => handleFormChange("EMAIL_HOST_PASSWORD", value)}
            >
              <Label>Password</Label>
              <Input placeholder="Password" />
            </TextField>
          </div>
        </div>
        <div className="flex max-w-4xl items-center py-1 gap-4">
          <Button type="submit" isPending={isSubmitting} isDisabled={isSubmitting}>
            {({ isPending }) => (
              <>
                {isPending ? <Spinner color="current" size="sm" /> : null}
                {isPending ? "Saving..." : "Save changes"}
              </>
            )}
          </Button>
          <Button variant="secondary" isPending={isSubmitting} onPress={() => setIsSendTestEmailModalOpen(true)}>
            {({ isPending }) => (
              <>
                {isPending ? <Spinner color="current" size="sm" /> : null}
                Send test email
              </>
            )}
          </Button>
        </div>
      </Form>
    </div>
  );
};
