"use client";

import type { FC, ReactNode, MouseEvent } from "react";
import { useMemo, useState } from "react";
import Link from "next/link";
import { Monitor } from "lucide-react";
// heroui
import { Button, Description, Input, Label, Spinner, TextField, toast } from "@heroui/react";
// syncturtle imports
import type {
  TFormattedInstanceConfiguration,
  TInstanceGoogleAuthenticationConfigurationKeys,
} from "@syncturtle/types";
import { API_BASE_URL } from "@syncturtle/constants";
// store hooks
import { useInstance } from "@/hooks/store/use-instance";
// components
import { CodeBlock } from "@/components/common/code-block";
import { CopyField } from "@/components/common/copy-field";
import { ConfirmDiscardModal } from "@/components/common/confirm-discard-modal";
// types
import type { ICopyField } from "@/components/common/copy-field";

interface IInstanceGoogleConfigFormProps {
  config: TFormattedInstanceConfiguration;
}

type TGoogleConfigFormValues = Record<TInstanceGoogleAuthenticationConfigurationKeys, string>;

type TGoogleConfigFormErrors = Partial<Record<keyof TGoogleConfigFormValues, string>>;

type TGoogleConfigFormField = {
  key: keyof TGoogleConfigFormValues;
  type: "text" | "password";
  label: string;
  description: ReactNode;
  placeholder: string;
  required?: boolean;
};

const AUTHENTICATION_SETTINGS_PATH = "/authentication";

function buildInitialValues(config: TFormattedInstanceConfiguration): TGoogleConfigFormValues {
  return {
    GOOGLE_CLIENT_ID: config["GOOGLE_CLIENT_ID"] ?? "",
    GOOGLE_CLIENT_SECRET: config["GOOGLE_CLIENT_SECRET"] ?? "",
  };
}

function buildCallbackUri(originURL: string) {
  const cleanOrigin = originURL.replace(/\/+$/, "");
  return `${cleanOrigin}/auth/google/callback/`;
}

function isSameForm(left: TGoogleConfigFormValues, right: TGoogleConfigFormValues) {
  return left.GOOGLE_CLIENT_ID === right.GOOGLE_CLIENT_ID && left.GOOGLE_CLIENT_SECRET === right.GOOGLE_CLIENT_SECRET;
}

function validateGoogleConfigForm(values: TGoogleConfigFormValues): TGoogleConfigFormErrors {
  const errors: TGoogleConfigFormErrors = {};

  if (!values.GOOGLE_CLIENT_ID.trim()) {
    errors.GOOGLE_CLIENT_ID = "Client ID is required.";
  }

  if (!values.GOOGLE_CLIENT_SECRET.trim()) {
    errors.GOOGLE_CLIENT_SECRET = "Client secret is required.";
  }

  return errors;
}

function hasErrors(errors: TGoogleConfigFormErrors) {
  return Object.keys(errors).length > 0;
}

function getResponseValue(
  response: Array<{ key: string; value?: string }> | undefined,
  key: keyof TGoogleConfigFormValues,
  fallback: string
) {
  return response?.find((item) => item.key === key)?.value ?? fallback;
}

export const InstanceGoogleConfigForm: FC<IInstanceGoogleConfigFormProps> = (props) => {
  const { config } = props;
  // store hooks
  const { updateInstanceConfigurations } = useInstance();
  // states
  const [initialValues, setInitialValues] = useState<TGoogleConfigFormValues>(() => buildInitialValues(config));
  const [values, setValues] = useState<TGoogleConfigFormValues>(() => buildInitialValues(config));
  const [errors, setErrors] = useState<TGoogleConfigFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDiscardChangesModalOpen, setIsDiscardChangesModalOpen] = useState(false);

  const isDirty = !isSameForm(values, initialValues);
  const originURL = API_BASE_URL?.trim() || (typeof window !== "undefined" ? window.location.origin : "");

  const googleFormFields = useMemo<TGoogleConfigFormField[]>(
    () => [
      {
        key: "GOOGLE_CLIENT_ID",
        type: "text",
        label: "Client ID",
        description: (
          <>
            Your client ID lives in your Google API Console.{" "}
            <a
              tabIndex={-1}
              href="https://developers.google.com/identity/protocols/oauth2/javascript-implicit-flow#creatingcred"
              target="_blank"
              className="text-custom-primary-100 hover:underline"
              rel="noreferrer"
            >
              Learn more.
            </a>
          </>
        ),
        placeholder: "840195096245-0p2tstej9j5nc4l8o1ah2dqondscqc1g.apps.googleusercontent.com",
        required: true,
      },
      {
        key: "GOOGLE_CLIENT_SECRET",
        type: "password",
        label: "Client secret",
        description: (
          <>
            Your client secret should also be in your Google API Console{" "}
            <a
              tabIndex={-1}
              href="https://developers.google.com/identity/oauth2/web/guides/get-google-api-clientid"
              target="_blank"
              className="text-custom-primary-100 hover:underline"
              rel="noreferrer"
            >
              Learn more
            </a>
          </>
        ),
        placeholder: "GOCShX-ADp4cI0kPqav1gGCBg5bE02E",
        required: true,
      },
    ],
    []
  );

  const googleCommonServiceFields = useMemo<ICopyField[]>(
    () => [
      {
        key: "origin_URL",
        label: "Origin URL",
        url: originURL,
        description: (
          <>
            We will auto-generate this. Paste this into the{" "}
            <CodeBlock darkerShade>Authorized JavaScript origins</CodeBlock> field. For this OAuth client{" "}
            <a
              tabIndex={-1}
              href="https://console.cloud.google.com/apis/credentials/oauthclient"
              target="_blank"
              className="text-custom-primary-100 hover:underline"
              rel="noreferrer"
            >
              here.
            </a>
          </>
        ),
      },
    ],
    [originURL]
  );

  const googleWebServiceFields = useMemo<ICopyField[]>(
    () => [
      {
        key: "callback_URI",
        label: "Callback URI",
        url: buildCallbackUri(originURL),
        description: (
          <>
            We will auto-generate this. Paste this into the <CodeBlock darkerShade>Authorized Redirect URI</CodeBlock>{" "}
            field. For this OAuth client{" "}
            <a
              tabIndex={-1}
              href="https://console.cloud.google.com/apis/credentials/oauthclient"
              target="_blank"
              className="text-custom-primary-100 hover:underline"
              rel="noreferrer"
            >
              here.
            </a>
          </>
        ),
      },
    ],
    [originURL]
  );

  const handleFormChange = (key: keyof TGoogleConfigFormValues, value: string) => {
    setValues((prev) => ({ ...prev, [key]: value }));

    setErrors((prev) => {
      if (!prev[key]) return prev;

      const next = { ...prev };
      delete next[key];

      return next;
    });
  };

  const handleSubmit = async () => {
    const nextErrors = validateGoogleConfigForm(values);

    setErrors(nextErrors);

    if (hasErrors(nextErrors)) return;

    setIsSubmitting(true);

    try {
      const payload: Partial<TGoogleConfigFormValues> = {
        GOOGLE_CLIENT_ID: values.GOOGLE_CLIENT_ID,
        GOOGLE_CLIENT_SECRET: values.GOOGLE_CLIENT_SECRET,
      };

      const response = await updateInstanceConfigurations(payload);

      const nextValues: TGoogleConfigFormValues = {
        GOOGLE_CLIENT_ID: getResponseValue(response, "GOOGLE_CLIENT_ID", values.GOOGLE_CLIENT_ID),
        GOOGLE_CLIENT_SECRET: getResponseValue(response, "GOOGLE_CLIENT_SECRET", values.GOOGLE_CLIENT_SECRET),
      };

      setValues(nextValues);
      setInitialValues(nextValues);
      setErrors({});

      toast("Done!", {
        actionProps: {
          children: "Dismiss",
          onPress: () => toast.clear(),
        },
        description: "Your GitHub authentication is configured. You should test it now.",
        variant: "success",
      });
    } catch (error) {
      console.error(error);

      toast("Something went wrong", {
        actionProps: {
          children: "Dismiss",
          onPress: () => toast.clear(),
        },
        description: "You GitHub authentication settings were not saved.",
        variant: "danger",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleGoBack = (event: MouseEvent<HTMLAnchorElement>) => {
    if (!isDirty) return;

    event.preventDefault();
    setIsDiscardChangesModalOpen(true);
  };

  return (
    <>
      <ConfirmDiscardModal
        isOpen={isDiscardChangesModalOpen}
        onDiscardHref={AUTHENTICATION_SETTINGS_PATH}
        handleClose={() => setIsDiscardChangesModalOpen(false)}
      />
      <div className="flex flex-col gap-8">
        <div className="grid grid-cols-2 gap-x-12 gap-y-8 w-full">
          <div className="flex flex-col gap-y-4 col-span-2 md:col-span-1 pt-1">
            <div className="pt-2.5 text-xl font-medium">Google-provided details for Syncturtle</div>
            {googleFormFields.map((field) => (
              <TextField
                key={field.key}
                isRequired={field.required}
                isInvalid={Boolean(errors[field.key])}
                type={field.type}
                name={field.key}
                value={values[field.key]}
                onChange={(value: string) => handleFormChange(field.key, value)}
              >
                <Label>{field.label}</Label>
                <Input placeholder={field.placeholder} />
                <Description>{field.description}</Description>
                {errors[field.key] ? (
                  <p className="text-sm text-danger" role="alert">
                    {errors[field.key]}
                  </p>
                ) : null}
              </TextField>
            ))}
            <div className="flex flex-col gap-1 pt-4">
              <div className="flex items-center gap-4">
                <Button
                  type="button"
                  size="sm"
                  isDisabled={!isDirty || isSubmitting}
                  isPending={isSubmitting}
                  onPress={handleSubmit}
                >
                  {({ isPending }) => (
                    <>
                      {isPending ? <Spinner color="current" size="sm" /> : null}
                      {isPending ? "Saving..." : "Save changes"}
                    </>
                  )}
                </Button>
                <Button variant="tertiary" size="sm">
                  <Link href={AUTHENTICATION_SETTINGS_PATH} onClick={handleGoBack}>
                    Go back
                  </Link>
                </Button>
              </div>
            </div>
          </div>
          <div className="col-span-2 md:col-span-1 flex flex-col gap-y-6">
            <div className="pt-2 text-xl font-medium">Syncturtle-provided details for Google</div>

            <div className="flex flex-col gap-y-4">
              <div className="flex flex-col gap-y-4 px-6 py-4 bg-custom-background-80 rounded-lg">
                {googleCommonServiceFields.map((field) => (
                  <CopyField key={field.key} label={field.label} url={field.url} description={field.description} />
                ))}
              </div>

              <div className="flex flex-col rounded-lg overflow-hidden">
                <div className="px-6 py-3 bg-custom-background-80/60 font-medium text-xs uppercase flex items-center gap-x-3 text-custom-text-200">
                  <Monitor className="w-3 h-3" />
                  Web
                </div>
                <div className="px-6 py-4 flex flex-col gap-y-4 bg-custom-background-80">
                  {googleWebServiceFields.map((field) => (
                    <CopyField key={field.key} label={field.label} url={field.url} description={field.description} />
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </>
  );
};
