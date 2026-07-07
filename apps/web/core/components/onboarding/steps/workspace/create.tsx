"use client";

import type { FC, SyntheticEvent } from "react";
import { useEffect, useMemo, useState } from "react";
// heroui
import {
  Button,
  Description,
  FieldError,
  Form,
  Input,
  InputGroup,
  Label,
  Radio,
  RadioGroup,
  Spinner,
  TextField,
  toast,
} from "@heroui/react";
// syncturtle imports
import { ORGANIZATION_SIZE, RESTRICTED_URLS } from "@syncturtle/constants";
import type { TFieldErrors, IUser, IWorkspace } from "@syncturtle/types";
import { useTranslation } from "@syncturtle/i18n";
// hooks
import { useUserProfile, useUserSettings } from "@/hooks/store/user";
import { useWorkspace } from "@/hooks/store/use-workspace";
// components
import { CommonOnboardingHeader } from "../common";
// services
import { WorkspaceService } from "@/services/workspace.service";
import { getFieldErrors, getPublicErrorMessage, hasFieldErrors } from "@/helpers/error.helper";

interface IWorkspaceCreateStepProps {
  user: IUser | undefined;
  onComplete: (skipInvites?: boolean) => void;
  handleCurrentViewChange: () => void;
  hasInvitations?: boolean;
}

interface IWorkspaceCreateFormData {
  name: string;
  slug: string;
  organizationSize: string;
}

const defaultFormData: IWorkspaceCreateFormData = {
  name: "",
  slug: "",
  organizationSize: "",
};

type TWorkspaceCreateClientErrors = Partial<Record<keyof IWorkspaceCreateFormData, string>>;

const slugify = (value: string) => value.toLowerCase().trim().replace(/\s+/g, "-");

const validateWorkspaceName = (value: string) => {
  const name = value.trim();

  if (!name) return "This field is required";

  if (!/^[\w\s-]*$/.test(name)) {
    return "Workspace name can only contain letters, numbers, spaces, dashes, and underscores.";
  }

  if (name.length > 80) {
    return "Workspace name must be within 80 characters.";
  }

  return undefined;
};

const validateWorkspaceSlug = (value: string) => {
  const slug = value.trim();

  if (!slug) return "This field is required";

  if (slug.length > 48) {
    return "Workspace URL must be within 48 characters.";
  }

  if (!/^[a-zA-Z0-9_-]+$/.test(slug)) {
    return "Workspace URL can only contain letters, numbers, dashes, and underscores.";
  }

  if (RESTRICTED_URLS.includes(slug.toLowerCase())) {
    return "This workspace URL is reserved.";
  }

  return undefined;
};

const validateWorkspaceCreateForm = (formData: IWorkspaceCreateFormData) => {
  const errors: TWorkspaceCreateClientErrors = {};

  const nameError = validateWorkspaceName(formData.name);
  const slugError = validateWorkspaceSlug(formData.slug);

  if (nameError) errors.name = nameError;
  if (slugError) errors.slug = slugError;

  if (!formData.organizationSize.trim()) {
    errors.organizationSize = "This field is required";
  }

  return errors;
};

const workspaceService = new WorkspaceService();

export const WorkspaceCreateStep: FC<IWorkspaceCreateStepProps> = (props) => {
  const { user, onComplete, handleCurrentViewChange, hasInvitations = false } = props;
  // sote hooks
  const { updateUserProfile } = useUserProfile();
  const { fetchCurrentUserSettings } = useUserSettings();
  const { createWorkspace, fetchWorkspaces } = useWorkspace();
  const { t } = useTranslation();
  // states
  const [formData, setFormData] = useState<IWorkspaceCreateFormData>(defaultFormData);
  const [clientErrors, setClientErrors] = useState<TWorkspaceCreateClientErrors>({});
  const [serverFieldErrors, setServerFieldErrors] = useState<TFieldErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [wasSubmitted, setWasSubmitted] = useState(false);
  const [host, setHost] = useState("");

  useEffect(() => {
    setHost(window.location.host);
  }, []);

  const clearServerFieldErrors = (field: keyof IWorkspaceCreateFormData) => {
    setServerFieldErrors((prev) => {
      if (!prev[field]) return prev;

      const next = { ...prev };
      delete next[field];

      return next;
    });
  };

  const handleFormChange = (key: keyof IWorkspaceCreateFormData, value: string) => {
    clearServerFieldErrors(key);

    setFormData((prev) => {
      let next: IWorkspaceCreateFormData;

      if (key === "name") {
        next = { ...prev, name: value, slug: slugify(value) };

        setServerFieldErrors((previousErrors) => {
          if (!previousErrors.slug) return previousErrors;

          const nextErrors = { ...previousErrors };
          delete nextErrors.slug;

          return nextErrors;
        });
      } else if (key === "slug") {
        next = { ...prev, slug: slugify(value) };
      } else {
        next = { ...prev, [key]: value };
      }

      if (wasSubmitted) {
        setClientErrors(validateWorkspaceCreateForm(next));
      }

      return next;
    });
  };

  const completeSetup = async (workspaceId: string) => {
    if (!user) return;

    await updateUserProfile({ lastWorkspaceId: workspaceId });

    await fetchCurrentUserSettings();
  };

  const handleCreateWorkspace = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    setWasSubmitted(true);
    setServerFieldErrors({});

    if (isSubmitting) return;

    const nextClientErrors = validateWorkspaceCreateForm(formData);
    setClientErrors(nextClientErrors);

    if (Object.keys(nextClientErrors).length > 0) return;

    setIsSubmitting(true);

    const createWorkspacePromise = (async () => {
      const slugCheck = await workspaceService.workspaceSlugCheck(formData.slug);

      if (slugCheck.available !== true || RESTRICTED_URLS.includes(formData.slug)) {
        const nextServerErrors: TFieldErrors = {
          slug: "Workspace URL is already taken.",
        };

        setServerFieldErrors(nextServerErrors);

        throw new Error("Workspace URL is already taken.");
      }

      const workspacePayload: Partial<IWorkspace> = {
        name: formData.name.trim(),
        slug: formData.slug.trim(),
        organizationSize: formData.organizationSize,
      };

      const workspaceResponse = await createWorkspace(workspacePayload);

      await fetchWorkspaces();
      await completeSetup(workspaceResponse.id);

      onComplete(formData.organizationSize === "Just myself");

      return workspaceResponse;
    })();

    toast.promise(createWorkspacePromise, {
      loading: "Creating workspace...",
      success: "Workspace created successfully",
      error: (error) => getPublicErrorMessage(error, "Could not create workspace."),
    });

    try {
      await createWorkspacePromise;
    } catch (error) {
      console.log(error);
      const nextServerErrors = getFieldErrors(error);

      if (hasFieldErrors(nextServerErrors)) {
        setServerFieldErrors(nextServerErrors);
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // derived values
  const currentClientErrors = useMemo(() => validateWorkspaceCreateForm(formData), [formData]);
  const getFieldError = (field: keyof IWorkspaceCreateFormData) => clientErrors[field] ?? serverFieldErrors[field];
  const nameError = getFieldError("name");
  const slugError = getFieldError("slug");
  const organizationSizeError = getFieldError("organizationSize");

  const isButtonDisabled = useMemo(() => {
    if (isSubmitting) return true;
    if (Object.keys(currentClientErrors).length > 0) return true;
    if (hasFieldErrors(serverFieldErrors)) return true;

    return false;
  }, [currentClientErrors, serverFieldErrors, isSubmitting]);

  return (
    <Form
      className="flex flex-col gap-10"
      validationBehavior="aria"
      validationErrors={serverFieldErrors}
      onSubmit={handleCreateWorkspace}
    >
      <CommonOnboardingHeader title="Create your workspace" description="All your work - unified." />

      <div className="flex flex-col gap-8 toast--success">
        <TextField
          id="name"
          name="name"
          isRequired
          value={formData.name}
          isInvalid={Boolean(nameError)}
          onChange={(value: string) => handleFormChange("name", value)}
          variant="secondary"
        >
          <Label>{t("workspace_creation.form.name.label")}</Label>
          <Input type="text" placeholder="Enter workspace name" />
          {nameError && <FieldError>{nameError}</FieldError>}
        </TextField>

        <TextField
          id="slug"
          name="slug"
          isRequired
          value={formData.slug}
          isInvalid={Boolean(slugError)}
          onChange={(value: string) => handleFormChange("slug", value)}
          variant="secondary"
        >
          <Label>{t("workspace_creation.form.url.label")}</Label>
          <InputGroup>
            <InputGroup.Prefix>{host}/</InputGroup.Prefix>
            <InputGroup.Input placeholder={t("workspace_creation.form.url.placeholder")} />
          </InputGroup>
          {!slugError && <Description>{t("workspace_creation.form.url.edit_slug")}</Description>}
          {slugError && <FieldError>{slugError}</FieldError>}
        </TextField>

        <RadioGroup
          isRequired
          name="organizationSize"
          orientation="horizontal"
          value={formData.organizationSize}
          isInvalid={Boolean(organizationSizeError)}
          onChange={(value: string) => handleFormChange("organizationSize", value)}
          variant="secondary"
        >
          <Label>{t("workspace_creation.form.organization_size.label")}</Label>
          {ORGANIZATION_SIZE.map((size) => (
            <Radio key={size} value={size}>
              <Radio.Content>
                <Radio.Control>
                  <Radio.Indicator>
                    {({ isSelected }) =>
                      isSelected ? <span className="text-xs leading-none text-background">✓</span> : null
                    }
                  </Radio.Indicator>
                </Radio.Control>
                <span className="font-medium">{size}</span>
              </Radio.Content>
            </Radio>
          ))}
          {organizationSizeError && <FieldError>{organizationSizeError}</FieldError>}
        </RadioGroup>
      </div>

      <div className="flex flex-col gap-4">
        <Button
          type="submit"
          size="sm"
          fullWidth
          isPending={isSubmitting}
          isDisabled={isButtonDisabled}
          className="rounded-md"
        >
          {({ isPending }) => (isPending ? <Spinner color="current" /> : t("workspace_creation.button.default"))}
        </Button>

        {hasInvitations && (
          <Button
            type="button"
            size="sm"
            variant="tertiary"
            fullWidth
            onPress={handleCurrentViewChange}
            isDisabled={isSubmitting}
          >
            Join existing workspace
          </Button>
        )}
      </div>
    </Form>
  );
};
