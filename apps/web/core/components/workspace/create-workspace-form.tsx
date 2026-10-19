"use client";

import type { Dispatch, SetStateAction, FC, SyntheticEvent } from "react";
import { useEffect, useRef, useState } from "react";
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
// i18n
import { useTranslation } from "@syncturtle/i18n";
// hooks
import { useAppRouter } from "@/hooks/use-app-router";
import { useWorkspace } from "@/hooks/store/use-workspace";
// services
import { WorkspaceService } from "@/services/workspace.service";
// helpers
import { getFieldErrors, getPublicErrorMessage, hasFieldErrors } from "@/helpers/error.helper";
// constants
import { ORGANIZATION_SIZE, RESTRICTED_URLS } from "@syncturtle/constants";
// types
import type { IWorkspace, TFieldErrors } from "@syncturtle/types";

export interface IWorkspaceCreateFormData {
  name: string;
  slug: string;
  organizationSize: string;
}

interface ICreateWorkspaceForm {
  onSubmit?: (workspace: IWorkspace) => Promise<void>;
  defaultValues: IWorkspaceCreateFormData;
  setDefaultValues: Dispatch<SetStateAction<IWorkspaceCreateFormData>>;
  secondaryButton?: React.ReactNode;
  primaryButtonText?: {
    loading: string;
    default: string;
  };
}

const slugifyName = (name: string) =>
  name
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 48)
    .replace(/-+$/g, "");

const normalizeEditedSlug = (slug: string) => slug.toLowerCase().trim().replace(/\s+/g, "-");

const validateWorkspaceName = (value: string): string | undefined => {
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

const validateWorkspaceSlug = (value: string): string | undefined => {
  if (!value) return "This field is required";

  if (value.length < 3) {
    return "Workspace URL must have at least 3 characters.";
  }

  if (value.length > 48) {
    return "Workspace URL must be within 48 characters.";
  }

  if (!/^[a-z0-9-]+$/.test(value) || value.startsWith("-") || value.endsWith("-")) {
    return "Use lowercase letters, numbers, and hyphens, without a hyphen at either end.";
  }

  if (RESTRICTED_URLS.includes(value)) {
    return "This workspace URL is reserved.";
  }

  return undefined;
};

const validateWorkspaceCreateForm = (values: IWorkspaceCreateFormData): TFieldErrors => {
  const errors: TFieldErrors = {};

  const nameError = validateWorkspaceName(values.name);
  const slugError = validateWorkspaceSlug(values.slug);

  if (nameError) errors.name = nameError;
  if (slugError) errors.slug = slugError;

  if (!values.organizationSize.trim()) {
    errors.organizationSize = "This field is required";
  }

  return errors;
};

const workspaceService = new WorkspaceService();

export const CreateWorkspaceForm: FC<ICreateWorkspaceForm> = (props) => {
  const {
    onSubmit,
    defaultValues,
    setDefaultValues,
    secondaryButton,
    primaryButtonText = {
      loading: "workspace_creation.button.loading",
      default: "workspace_creation.button.default",
    },
  } = props;
  const { t } = useTranslation();
  // router
  const router = useAppRouter();
  // store hooks
  const { createWorkspace } = useWorkspace();
  // states
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isComplete, setIsComplete] = useState(false);
  const [wasSubmitted, setWasSubmitted] = useState(false);
  const [serverFieldErrors, setServerFieldErrors] = useState<TFieldErrors>({});
  const [touched, setTouched] = useState<Partial<Record<keyof IWorkspaceCreateFormData, boolean>>>({});
  const [host, setHost] = useState("");

  const submittingRef = useRef(false);
  const createdWorkspaceRef = useRef<IWorkspace | null>(null);

  const clearServerFieldErrors = (field: keyof IWorkspaceCreateFormData) => {
    setServerFieldErrors((previousErrors) => {
      const nextErrors = { ...previousErrors };
      delete nextErrors[field];

      // Editing the name also generates a new slug.
      if (field === "name") {
        delete nextErrors.slug;
      }

      return nextErrors;
    });
  };

  const handleFormChange = <K extends keyof IWorkspaceCreateFormData>(key: K, value: IWorkspaceCreateFormData[K]) => {
    if (createdWorkspaceRef.current) return;

    setDefaultValues((previousValues) => {
      if (key === "name") {
        return {
          ...previousValues,
          name: value,
          slug: slugifyName(value),
        };
      }

      if (key === "slug") {
        return {
          ...previousValues,
          slug: normalizeEditedSlug(value),
        };
      }

      return {
        ...previousValues,
        [key]: value,
      };
    });

    setTouched((previousTouched) => ({
      ...previousTouched,
      [key]: true,
      ...(key === "name" ? { slug: true } : {}),
    }));

    clearServerFieldErrors(key);
  };

  const getFieldError = (field: keyof IWorkspaceCreateFormData): string | undefined => {
    if (serverFieldErrors[field]) {
      return serverFieldErrors[field];
    }

    if (wasSubmitted || touched[field]) {
      return clientErrors[field];
    }

    return undefined;
  };

  const handleCreateWorkspace = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (submittingRef.current || isComplete) return;

    setWasSubmitted(true);

    if (!createdWorkspaceRef.current && hasFieldErrors(clientErrors)) {
      return;
    }

    submittingRef.current = true;
    setIsSubmitting(true);

    const isRetryAfterCreation = Boolean(createdWorkspaceRef.current);

    const createWorkspacePromise = (async () => {
      let workspace = createdWorkspaceRef.current;

      if (!workspace) {
        const slugCheck = await workspaceService.workspaceSlugCheck(defaultValues.slug);

        if (slugCheck.available !== true || RESTRICTED_URLS.includes(defaultValues.slug)) {
          const message = t("workspace_creation.errors.validation.url_already_taken");

          setServerFieldErrors({ slug: message });
          throw new Error(message);
        }

        const workspacePayload: Partial<IWorkspace> = {
          name: defaultValues.name.trim(),
          slug: defaultValues.slug,
          organizationSize: defaultValues.organizationSize,
        };

        workspace = await createWorkspace(workspacePayload);

        // If the page's onSubmit fails, the next attempt resumes with
        // this workspace instead of sending another creation request.
        createdWorkspaceRef.current = workspace;
      }

      if (onSubmit) {
        await onSubmit(workspace);
      }

      return workspace;
    })();

    toast.promise(createWorkspacePromise, {
      loading: isRetryAfterCreation ? "Finishing workspace setup..." : t(primaryButtonText.loading),
      success: isRetryAfterCreation ? "Workspace is ready." : t("workspace_creation.toast.success.message"),
      error: (error) =>
        getPublicErrorMessage(
          error,
          isRetryAfterCreation
            ? "Could not finish workspace setup. Please try again."
            : "Could not create workspace. Please try again."
        ),
    });

    try {
      await createWorkspacePromise;
      setIsComplete(true);
    } catch (error) {
      // Field violations from the creation API belong on the form.
      // A later profile/navigation failure can be retried without
      // changing the already-created workspace.
      if (!createdWorkspaceRef.current) {
        const nextFieldErrors = getFieldErrors(error);

        if (hasFieldErrors(nextFieldErrors)) {
          setServerFieldErrors(nextFieldErrors);
        }
      }
    } finally {
      submittingRef.current = false;
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    setHost(window.location.host);
  }, []);

  // derived values
  const clientErrors = validateWorkspaceCreateForm(defaultValues);

  const nameError = getFieldError("name");
  const slugError = getFieldError("slug");
  const organizationSizeError = getFieldError("organizationSize");
  const isButtonDisabled =
    isSubmitting ||
    isComplete ||
    (!createdWorkspaceRef.current && (hasFieldErrors(clientErrors) || hasFieldErrors(serverFieldErrors)));
  const fieldsDisabled = isSubmitting || isComplete || Boolean(createdWorkspaceRef.current);

  return (
    <Form
      className="space-y-6 sm:space-y-9"
      validationBehavior="aria"
      validationErrors={serverFieldErrors}
      onSubmit={handleCreateWorkspace}
    >
      <div className="space-y-6 sm:space-y-7">
        <TextField
          name="name"
          value={defaultValues.name}
          isInvalid={Boolean(nameError)}
          isDisabled={fieldsDisabled}
          onChange={(value) => handleFormChange("name", value)}
        >
          <Label>{t("workspace_creation.form.name.label")}</Label>
          <Input type="text" variant="secondary" placeholder={t("workspace_creation.form.name.placeholder")} />
          {nameError && <FieldError>{nameError}</FieldError>}
        </TextField>

        <TextField
          name="slug"
          isRequired
          value={defaultValues.slug}
          isInvalid={Boolean(slugError)}
          isDisabled={fieldsDisabled}
          onChange={(value) => handleFormChange("slug", value)}
        >
          <Label>{t("workspace_creation.form.url.label")}</Label>
          <InputGroup variant="secondary">
            <InputGroup.Prefix>{host}/</InputGroup.Prefix>
            <InputGroup.Input placeholder={t("workspace_creation.form.url.placeholder")} />
          </InputGroup>
          {!slugError && <Description>{t("workspace_creation.form.url.edit_slug")}</Description>}
          {slugError && <FieldError>{slugError}</FieldError>}
        </TextField>

        <RadioGroup
          name="organizationSize"
          isRequired
          orientation="horizontal"
          variant="secondary"
          value={defaultValues.organizationSize}
          isInvalid={Boolean(organizationSizeError)}
          isDisabled={fieldsDisabled}
          onChange={(value) => handleFormChange("organizationSize", value)}
        >
          <Label>{t("workspace_creation.form.organization_size.label")}</Label>

          {ORGANIZATION_SIZE.map((size) => (
            <Radio key={size} value={size}>
              <Radio.Content>
                <Radio.Control>
                  <Radio.Indicator />
                </Radio.Control>
                <span className="font-medium">{size}</span>
              </Radio.Content>
            </Radio>
          ))}

          {organizationSizeError && <FieldError>{organizationSizeError}</FieldError>}
        </RadioGroup>
      </div>

      <div className="flex flex-wrap items-center gap-4">
        {secondaryButton}

        <Button type="submit" variant="primary" isPending={isSubmitting} isDisabled={isButtonDisabled}>
          {({ isPending }) =>
            isPending ? (
              <>
                <Spinner color="current" />
                {t(primaryButtonText.loading)}
              </>
            ) : createdWorkspaceRef.current ? (
              t("common.continue")
            ) : (
              t(primaryButtonText.default)
            )
          }
        </Button>

        {!secondaryButton && (
          <Button type="button" variant="secondary" onPress={() => router.back()} isDisabled={isSubmitting}>
            Go back
          </Button>
        )}
      </div>
    </Form>
  );
};
