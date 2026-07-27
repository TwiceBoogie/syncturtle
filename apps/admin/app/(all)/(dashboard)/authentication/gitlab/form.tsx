"use client";

import type { FC, ReactNode, MouseEvent } from "react";
import { useMemo, useState } from "react";
// heroui
import { Button, Description, Input, Label, Spinner, TextField, toast } from "@heroui/react";
// hooks
import { useInstance } from "@/hooks/store/use-instance";
// constants
import { API_BASE_URL } from "@syncturtle/constants";
// types
import type {
  TFormattedInstanceConfiguration,
  TInstanceGitlabAuthenticationConfigurationKeys,
} from "@syncturtle/types";
import { CodeBlock } from "@/components/common/code-block";
import { CopyField } from "@/components/common/copy-field";
import type { ICopyField } from "@/components/common/copy-field";
import { ConfirmDiscardModal } from "@/components/common/confirm-discard-modal";
import Link from "next/link";

interface IInstanceGitlabConfigFormProps {
  config: TFormattedInstanceConfiguration;
}

type TGitlabConfigFormValues = Record<TInstanceGitlabAuthenticationConfigurationKeys, string>;

type TGitlabConfigFormErrors = Partial<Record<keyof TGitlabConfigFormValues, string>>;

type TGitlabConfigFormField = {
  key: keyof TGitlabConfigFormValues;
  type: "text" | "password";
  label: string;
  description: ReactNode;
  placeholder: string;
  required?: boolean;
};

const AUTHENTICATION_SETTINGS_PATH = "/authentication";

function buildInitialValues(config: TFormattedInstanceConfiguration): TGitlabConfigFormValues {
  return {
    GITLAB_HOST: config["GITLAB_HOST"] ?? "",
    GITLAB_CLIENT_ID: config["GITLAB_CLIENT_ID"] ?? "",
    GITLAB_CLIENT_SECRET: config["GITLAB_CLIENT_SECRET"] ?? "",
  };
}

function buildCallbackUri(originURL: string) {
  const cleanOrigin = originURL.replace(/\/+$/, "");
  return `${cleanOrigin}/auth/gitlab/callback/`;
}

function isSameForm(left: TGitlabConfigFormValues, right: TGitlabConfigFormValues) {
  return (
    left.GITLAB_HOST === right.GITLAB_HOST &&
    left.GITLAB_CLIENT_ID === right.GITLAB_CLIENT_ID &&
    left.GITLAB_CLIENT_SECRET === right.GITLAB_CLIENT_SECRET
  );
}

function validateGitlabConfigForm(values: TGitlabConfigFormValues): TGitlabConfigFormErrors {
  const errors: TGitlabConfigFormErrors = {};

  if (!values.GITLAB_HOST.trim()) {
    errors.GITLAB_HOST = "Client host is required.";
  }

  if (!values.GITLAB_CLIENT_ID.trim()) {
    errors.GITLAB_CLIENT_ID = "Client ID is required.";
  }

  if (!values.GITLAB_CLIENT_SECRET.trim()) {
    errors.GITLAB_CLIENT_SECRET = "Client secret is required.";
  }

  return errors;
}

function hasErrors(errors: TGitlabConfigFormErrors) {
  return Object.keys(errors).length > 0;
}

function getResponseValue(
  response: Array<{ key: string; value?: string }> | undefined,
  key: keyof TGitlabConfigFormValues,
  fallback: string
) {
  return response?.find((item) => item.key === key)?.value ?? fallback;
}

export const InstanceGitlabConfigForm: FC<IInstanceGitlabConfigFormProps> = (props) => {
  const { config } = props;
  // store hooks
  const { updateInstanceConfigurations } = useInstance();
  // states
  const [initialValues, setInitialValues] = useState<TGitlabConfigFormValues>(() => buildInitialValues(config));
  const [values, setValues] = useState<TGitlabConfigFormValues>(() => buildInitialValues(config));
  const [errors, setErrors] = useState<TGitlabConfigFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDiscardChangesModalOpen, setIsDiscardChangesModalOpen] = useState(false);
  // const [formData, setFormData] = useState<TGitlabConfigFormValues>({
  //   GITLAB_HOST: config["GITLAB_HOST"] ?? "",
  //   GITLAB_CLIENT_ID: config["GITLAB_CLIENT_ID"] ?? "",
  //   GITLAB_CLIENT_SECRET: config["GITLAB_CLIENT_SECRET"] ?? "",
  // });

  const isDirty = !isSameForm(values, initialValues);
  const originURL = API_BASE_URL?.trim() || (typeof window !== "undefined" ? window.location.origin : "");

  const gitlabFormFields = useMemo<TGitlabConfigFormField[]>(
    () => [
      {
        key: "GITLAB_HOST",
        type: "text",
        label: "Host",
        description: (
          <>
            This is either https://gitlab.com or the <CodeBlock>domain.tld</CodeBlock> where you host GitLab.
          </>
        ),
        placeholder: "https://gitlab.com",
        required: false,
      },
      {
        key: "GITLAB_CLIENT_ID",
        type: "text",
        label: "Application ID",
        description: (
          <>
            Get this from your{" "}
            <a
              tabIndex={-1}
              href="https://docs.gitlab.com/ee/integration/oauth_provider.html"
              target="_blank"
              className="text-custom-primary-100 hover:underline"
              rel="noreferrer"
            >
              GitLab OAuth application settings.
            </a>
          </>
        ),
        placeholder: "c2ef2e7fc4e9d15aa7630f5637d59e8e4a27ff01dceebdb26b0d267b9adcf3c3",
        required: true,
      },
      {
        key: "GITLAB_CLIENT_SECRET",
        type: "password",
        label: "Client secret",
        description: (
          <>
            Your client secret is also found in your{" "}
            <a
              tabIndex={-1}
              href="https://docs.gitlab.com/ee/integration/oauth_provider.html"
              target="_blank"
              className="text-custom-primary-100 hover:underline"
              rel="noreferrer"
            >
              GitLab OAuth application settings.
            </a>
          </>
        ),
        placeholder: "9b0050f94ec1b744e32ce79ea4ffacd40d4119cb",
        required: true,
      },
    ],
    []
  );

  const gitlabWebServiceFields = useMemo<ICopyField[]>(
    () => [
      {
        key: "callback_URI",
        label: "Callback URI",
        url: buildCallbackUri(originURL),
        description: (
          <>
            We will auto-generate this. Paste this into the <CodeBlock darkerShade>Redirect URI</CodeBlock> field of
            your{" "}
            <a
              tabIndex={-1}
              href="https://github.com/settings/applications/new"
              target="_blank"
              className="text-custom-primary-100 hover:underline"
              rel="noreferrer"
            >
              GitLab OAuth application.
            </a>
          </>
        ),
      },
    ],
    [originURL]
  );

  const handleFormChange = (key: keyof TGitlabConfigFormValues, value: string) => {
    setValues((prev) => ({ ...prev, [key]: value }));

    setErrors((prev) => {
      if (!prev[key]) return prev;

      const next = { ...prev };
      delete next[key];

      return next;
    });
  };

  const handleSubmit = async () => {
    const nextErrors = validateGitlabConfigForm(values);

    setErrors(nextErrors);

    if (hasErrors(nextErrors)) return;

    setIsSubmitting(true);

    try {
      const payload: Partial<TGitlabConfigFormValues> = {
        GITLAB_HOST: values.GITLAB_HOST,
        GITLAB_CLIENT_ID: values.GITLAB_CLIENT_ID,
        GITLAB_CLIENT_SECRET: values.GITLAB_CLIENT_SECRET,
      };

      const response = await updateInstanceConfigurations(payload);

      const nextValues: TGitlabConfigFormValues = {
        GITLAB_HOST: getResponseValue(response, "GITLAB_HOST", values.GITLAB_HOST),
        GITLAB_CLIENT_ID: getResponseValue(response, "GITLAB_CLIENT_ID", values.GITLAB_CLIENT_ID),
        GITLAB_CLIENT_SECRET: getResponseValue(response, "GITLAB_CLIENT_SECRET", values.GITLAB_CLIENT_SECRET),
      };

      setValues(nextValues);
      setInitialValues(nextValues);
      setErrors({});

      toast("Done!", {
        actionProps: {
          children: "Dismiss",
          onPress: () => toast.clear(),
        },
        description: "Your GitLab authentication is configured. You should test it now.",
        variant: "success",
      });
    } catch (error) {
      console.error(error);

      toast("Something went wrong", {
        actionProps: {
          children: "Dismiss",
          onPress: () => toast.clear(),
        },
        description: "You GitLab authentication settings were not saved.",
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
            <div className="pt-2.5 text-xl font-medium">GitLab-provided details for Syncturtle</div>
            {gitlabFormFields.map((field) => (
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
          <div className="col-span-2 md:col-span-1">
            <div className="flex flex-col gap-y-4 px-6 pt-1.5 pb-4 bg-custom-background-80/60 rounded-lg">
              <div className="pt-2 text-xl font-medium">Syncturtle-provided details for GitLab</div>
              {gitlabWebServiceFields.map((field) => (
                <CopyField key={field.key} label={field.label} url={field.url} description={field.description} />
              ))}
            </div>
          </div>
        </div>
      </div>
    </>
  );
};
