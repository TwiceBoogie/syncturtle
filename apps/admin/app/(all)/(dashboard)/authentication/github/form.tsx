"use client";

import type { FC, ReactNode, MouseEvent } from "react";
import { useMemo, useState } from "react";
import Link from "next/link";
// heroui
import { Button, Description, Input, Label, Spinner, TextField, toast } from "@heroui/react";
import { Monitor } from "lucide-react";
// hooks
import { useInstance } from "@/hooks/store/use-instance";
// components
import { CopyField } from "@/components/common/copy-field";
import type { ICopyField } from "@/components/common/copy-field";
import { CodeBlock } from "@/components/common/code-block";
import { ConfirmDiscardModal } from "@/components/common/confirm-discard-modal";
// constants
import { API_BASE_URL } from "@syncturtle/constants";
// types
import type {
  TFormattedInstanceConfiguration,
  TInstanceGithubAuthenticationConfigurationKeys,
} from "@syncturtle/types";

interface IInstanceGithubConfigFormProps {
  config: TFormattedInstanceConfiguration;
}

type TGithubConfigFormValues = Record<TInstanceGithubAuthenticationConfigurationKeys, string>;

type TGithubConfigFormErrors = Partial<Record<keyof TGithubConfigFormValues, string>>;

type TGithubConfigFormField = {
  key: keyof TGithubConfigFormValues;
  type: "text" | "password";
  label: string;
  description: ReactNode;
  placeholder: string;
  required?: boolean;
};

const AUTHENTICATION_SETTINGS_PATH = "/authentication";

function buildInitialValues(config: TFormattedInstanceConfiguration): TGithubConfigFormValues {
  return {
    GITHUB_CLIENT_ID: config["GITHUB_CLIENT_ID"] ?? "",
    GITHUB_CLIENT_SECRET: config["GITHUB_CLIENT_SECRET"] ?? "",
    GITHUB_ORGANIZATION_ID: config["GITHUB_ORGANIZATION_ID"] ?? "",
  };
}

function buildCallbackUri(originURL: string) {
  const cleanOrigin = originURL.replace(/\/+$/, "");
  return `${cleanOrigin}/auth/github/callback/`;
}

function isSameForm(left: TGithubConfigFormValues, right: TGithubConfigFormValues) {
  return (
    left.GITHUB_CLIENT_ID === right.GITHUB_CLIENT_ID &&
    left.GITHUB_CLIENT_SECRET === right.GITHUB_CLIENT_SECRET &&
    left.GITHUB_ORGANIZATION_ID === right.GITHUB_ORGANIZATION_ID
  );
}

function validateGithubConfigForm(values: TGithubConfigFormValues): TGithubConfigFormErrors {
  const errors: TGithubConfigFormErrors = {};

  if (!values.GITHUB_CLIENT_ID.trim()) {
    errors.GITHUB_CLIENT_ID = "Client ID is required.";
  }

  if (!values.GITHUB_CLIENT_SECRET.trim()) {
    errors.GITHUB_CLIENT_SECRET = "Client secret is required.";
  }

  return errors;
}

function hasErrors(errors: TGithubConfigFormErrors) {
  return Object.keys(errors).length > 0;
}

function getResponseValue(
  response: Array<{ key: string; value?: string }> | undefined,
  key: keyof TGithubConfigFormValues,
  fallback: string
) {
  return response?.find((item) => item.key === key)?.value ?? fallback;
}

export const InstanceGithubConfigForm: FC<IInstanceGithubConfigFormProps> = (props) => {
  const { config } = props;
  // store hooks
  const { updateInstanceConfigurations } = useInstance();
  // states
  const [initialValues, setInitialValues] = useState<TGithubConfigFormValues>(() => buildInitialValues(config));
  const [values, setValues] = useState<TGithubConfigFormValues>(() => buildInitialValues(config));
  const [errors, setErrors] = useState<TGithubConfigFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDiscardChangesModalOpen, setIsDiscardChangesModalOpen] = useState(false);
  // const [formData, setFormData] = useState<TGithubConfigFormValues>({
  //   GITHUB_CLIENT_ID: config["GITHUB_CLIENT_ID"] ?? "",
  //   GITHUB_CLIENT_SECRET: config["GITHUB_CLIENT_SECRET"] ?? "",
  //   GITHUB_ORGANIZATION_ID: config["GITHUB_ORGANIZATION_ID"] ?? "",
  // });

  const isDirty = !isSameForm(values, initialValues);
  const originURL = API_BASE_URL?.trim() || (typeof window !== "undefined" ? window.location.origin : "");

  const githubFormFields = useMemo<TGithubConfigFormField[]>(
    () => [
      {
        key: "GITHUB_CLIENT_ID",
        type: "text",
        label: "Client ID",
        description: (
          <>
            You will get this from your{" "}
            <a
              tabIndex={-1}
              href="https://github.com/settings/applications/new"
              target="_blank"
              className="text-custom-primary-100 hover:underline"
              rel="noreferrer"
            >
              Github OAuth application settings.
            </a>
          </>
        ),
        placeholder: "70a44354520df8bd9bcd",
        required: true,
      },
      {
        key: "GITHUB_CLIENT_SECRET",
        type: "password",
        label: "Client secret",
        description: (
          <>
            Your client secret is also found in your{" "}
            <a
              tabIndex={-1}
              href="https://github.com/settings/applications/new"
              target="_blank"
              className="text-custom-primary-100 hover:underline"
              rel="noreferrer"
            >
              Github OAuth application settings.
            </a>
          </>
        ),
        placeholder: "9b0050f94ec1b744e32ce79ea4ffacd40d4119cb",
        required: true,
      },
      {
        key: "GITHUB_ORGANIZATION_ID",
        type: "text",
        label: "Organization ID",
        description: <>The organization GitHub ID.</>,
        placeholder: "123456789",
        required: false,
      },
    ],
    []
  );

  const githubCommonServiceFields = useMemo<ICopyField[]>(
    () => [
      {
        key: "origin_URL",
        label: "Origin URL",
        url: originURL,
        description: (
          <>
            We will auto-generate this. Paste this into the <CodeBlock darkerShade>Authorized origin URL</CodeBlock>{" "}
            field{" "}
            <a
              tabIndex={-1}
              href="https://github.com/settings/applications/new"
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

  const githubWebServiceFields = useMemo<ICopyField[]>(
    () => [
      {
        key: "callback_URI",
        label: "Callback URI",
        url: buildCallbackUri(originURL),
        description: (
          <>
            We will auto-generate this. Paste this into the <CodeBlock darkerShade>Authorized Callback URI</CodeBlock>{" "}
            field{" "}
            <a
              tabIndex={-1}
              href="https://github.com/settings/applications/new"
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

  const handleFormChange = (key: keyof TGithubConfigFormValues, value: string) => {
    setValues((prev) => ({ ...prev, [key]: value }));

    setErrors((prev) => {
      if (!prev[key]) return prev;

      const next = { ...prev };
      delete next[key];

      return next;
    });
  };

  const handleSubmit = async () => {
    const nextErrors = validateGithubConfigForm(values);

    setErrors(nextErrors);

    if (hasErrors(nextErrors)) return;

    setIsSubmitting(true);

    try {
      const payload: Partial<TGithubConfigFormValues> = {
        GITHUB_CLIENT_ID: values.GITHUB_CLIENT_ID,
        GITHUB_CLIENT_SECRET: values.GITHUB_CLIENT_SECRET,
        GITHUB_ORGANIZATION_ID: values.GITHUB_ORGANIZATION_ID,
      };

      const response = await updateInstanceConfigurations(payload);

      const nextValues: TGithubConfigFormValues = {
        GITHUB_CLIENT_ID: getResponseValue(response, "GITHUB_CLIENT_ID", values.GITHUB_CLIENT_ID),
        GITHUB_CLIENT_SECRET: getResponseValue(response, "GITHUB_CLIENT_SECRET", values.GITHUB_CLIENT_SECRET),
        GITHUB_ORGANIZATION_ID: getResponseValue(response, "GITHUB_ORGANIZATION_ID", values.GITHUB_ORGANIZATION_ID),
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
            <div className="pt-2.5 text-xl font-medium">Github-provided details for Syncturtle</div>
            {githubFormFields.map((field) => (
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
            <div className="pt-2 text-xl font-medium">Syncturtle-provided details for GitHub</div>

            <div className="flex flex-col gap-y-4">
              <div className="flex flex-col gap-y-4 px-6 py-4 bg-custom-background-80 rounded-lg">
                {githubCommonServiceFields.map((field) => (
                  <CopyField key={field.key} label={field.label} url={field.url} description={field.description} />
                ))}
              </div>

              <div className="flex flex-col overflow-hidden rounded-lg">
                <div className="px-6 py-3 bg-custom-background-80/60 font-medium text-xs uppercase flex items-center gap-x-3 text-custom-text-200">
                  <Monitor className="w-3 h-3" />
                  Web
                </div>
                <div className="px-6 py-4 flex flex-col gap-y-4 bg-custom-background-80">
                  {githubWebServiceFields.map((field) => (
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
