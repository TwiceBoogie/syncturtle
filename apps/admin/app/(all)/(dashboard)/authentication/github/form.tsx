"use client";

import { FC, useState } from "react";
// heroui
import { Button, Description, Input, Label, Spinner, TextField } from "@heroui/react";
// types
import type {
  TFormattedInstanceConfiguration,
  TInstanceGithubAuthenticationConfigurationKeys,
} from "@syncturtle/types";
// import { useInstance } from "@/hooks/store/use-instance";
import { CopyField, ICopyField } from "@/components/common/copy-field";
import { API_BASE_URL } from "@syncturtle/constants";
import { isEmpty } from "lodash";
import { CodeBlock } from "@/components/common/code-block";

interface IInstanceGithubConfigFormProps {
  config: TFormattedInstanceConfiguration;
}

type TGithubConfigFormValues = Record<TInstanceGithubAuthenticationConfigurationKeys, string>;

export const InstanceGithubConfigForm: FC<IInstanceGithubConfigFormProps> = (props) => {
  const { config } = props;
  // store hooks
  // const { updateInstanceConfigurations } = useInstance();
  // states
  // const [isSubmitting, setIsSubmitting] = useState(false);
  const [formData, setFormData] = useState<TGithubConfigFormValues>({
    GITHUB_CLIENT_ID: config["GITHUB_CLIENT_ID"] ?? "",
    GITHUB_CLIENT_SECRET: config["GITHUB_CLIENT_SECRET"] ?? "",
    GITHUB_ORGANIZATION_ID: config["GITHUB_ORGANIZATION_ID"] ?? "",
  });

  const handleFormChange = (key: keyof TGithubConfigFormValues, value: string) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  const originURL = !isEmpty(API_BASE_URL) ? API_BASE_URL : typeof window !== "undefined" ? window.location.origin : "";

  const GITHUB_SERVICE_FIELD: ICopyField[] = [
    {
      key: "origin_URL",
      label: "Origin URL",
      url: originURL,
      description: (
        <>
          We will auto-generate this. Paste this into the <CodeBlock darkerShade>Authorized origin URL</CodeBlock> field{" "}
          <a
            tabIndex={-1}
            href="#"
            target="_blank"
            className="text-custom-primary-100 hover:underline"
            rel="noreferrer"
          >
            here.
          </a>
        </>
      ),
    },
    {
      key: "callback_URI",
      label: "Callback URI",
      url: `${originURL}/auth/github/callback`,
      description: (
        <>
          We will auto-generate this. Paste this into your <CodeBlock darkerShade>Authorized Callback URI</CodeBlock>{" "}
          field{" "}
          <a
            tabIndex={-1}
            href="#"
            target="_blank"
            className="text-custom-primary-100 hover:underline"
            rel="noreferrer"
          >
            here.
          </a>
        </>
      ),
    },
  ];
  return (
    <>
      <div className="flex flex-col gap-8">
        <div className="grid grid-cols-2 gap-x-12 gap-y-8 w-full">
          <div className="flex flex-col gap-y-4 col-span-2 md:col-span-1 pt-1">
            <div className="pt-2.5 text-xl font-medium">Github-provided details for Syncturtle</div>
            <TextField
              isRequired
              type="text"
              name="GITHUB_CLIENT_ID"
              value={formData.GITHUB_CLIENT_ID}
              onChange={(value: string) => handleFormChange("GITHUB_CLIENT_ID", value)}
            >
              <Label>Client ID</Label>
              <Input placeholder="70a44354520df8bd9bcd" />
              <Description>
                You will get this from your{" "}
                <a
                  tabIndex={-1}
                  href="https://github.com/settings/applications/new"
                  target="_blank"
                  className="text-custom-primary-100 hover:underline"
                  rel="noreferrer"
                >
                  GitHub OAuth application settings.
                </a>
              </Description>
            </TextField>
            <TextField
              isRequired
              type="password"
              name="GITHUB_CLIENT_SECRET"
              value={formData.GITHUB_CLIENT_SECRET}
              onChange={(value: string) => handleFormChange("GITHUB_CLIENT_SECRET", value)}
            >
              <Label>Client secret</Label>
              <Input placeholder="9b0050f94ec1b744e32ce79ea4ffacd40d4119cb" />
              <Description>
                You will get this from your{" "}
                <a
                  tabIndex={-1}
                  href="https://github.com/settings/applications/new"
                  target="_blank"
                  className="text-custom-primary-100 hover:underline"
                  rel="noreferrer"
                >
                  GitHub OAuth application settings.
                </a>
              </Description>
            </TextField>
            <TextField
              isRequired
              type="text"
              name="GITHUB_ORGANIZATION_ID"
              value={formData.GITHUB_ORGANIZATION_ID}
              onChange={(value: string) => handleFormChange("GITHUB_ORGANIZATION_ID", value)}
            >
              <Label>Organization ID</Label>
              <Input placeholder="123456789" />
              <Description>The organization GitHub ID.</Description>
            </TextField>
            <div className="flex flex-col gap-1 pt-4">
              <div className="flex items-center gap-4">
                <Button type="submit" isPending size="sm">
                  {({ isPending }) => (
                    <>
                      {isPending ? <Spinner color="current" size="sm" /> : null}
                      {isPending ? "Saving..." : "Save changes"}
                    </>
                  )}
                </Button>
                <Button variant="tertiary" size="sm">
                  Go back
                </Button>
              </div>
            </div>
          </div>
          <div className="col-span-2 md:col-span-1">
            <div className="flex flex-col gap-y-4 px-6 pt-1.5 pb-4 bg-custom-background-80/60 rounded-lg">
              <div className="pt-2 text-xl font-medium">Syncturtle-provided details for GitHub</div>
              {GITHUB_SERVICE_FIELD.map((field) => (
                <CopyField key={field.key} label={field.label} url={field.url} description={field.description} />
              ))}
            </div>
          </div>
        </div>
      </div>
    </>
  );
};
