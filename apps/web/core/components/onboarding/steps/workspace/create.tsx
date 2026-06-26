"use client";

import type { FC, FormEvent } from "react";
import { useEffect, useState } from "react";
// heroui
import { Form, toast } from "@heroui/react";
// syncturtle imports
import { RESTRICTED_URLS } from "@syncturtle/constants";
import type { IUser, IWorkspace } from "@syncturtle/types";
import { WorkspaceService } from "@/services/workspace.service";
import { useUserProfile, useUserSettings } from "@/hooks/store/user";
import { useWorkspace } from "@/hooks/store/use-workspace";
import { CommonOnboardingHeader } from "../common";

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

// const slugify = (value: string) => value.toLowerCase().trim().replace(/\s+/g, "-");

const validateWorkspaceName = (value: string) => {
  const name = value.trim();

  if (!name) return "This field is required";

  if (!/^[\w\s-]*$/.test(name)) {
    return "Workspace name can only contain letters, numbers, spaces, dashes, and underscores.";
  }

  if (name.length > 80) {
    return "Workspace name must be within 80 characters.";
  }

  return null;
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

  return null;
};

const workspaceService = new WorkspaceService();

export const WorkspaceCreateStep: FC<IWorkspaceCreateStepProps> = (props) => {
  const { user, onComplete, handleCurrentViewChange, hasInvitations = false } = props;
  console.log(handleCurrentViewChange, hasInvitations);
  // sote hooks
  const { updateUserProfile } = useUserProfile();
  const { fetchCurrentUserSettings } = useUserSettings();
  const { createWorkspace, fetchWorkspaces } = useWorkspace();
  // states
  const [formData, setFormData] = useState<IWorkspaceCreateFormData>(defaultFormData);
  const [slugError, setSlugError] = useState<string | undefined>(undefined);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [wasSubmitted, setWasSubmitted] = useState(false);
  const [host, setHost] = useState("");
  console.log(`${host} ${wasSubmitted} ${slugError} ${setFormData}`);

  useEffect(() => {
    setHost(window.location.host);
  }, []);

  // const handleFormChange = (key: keyof IWorkspaceCreateFormData, value: string) => {
  //   setSlugError(undefined);

  //   setFormData((prev) => {
  //     if (key === "name") {
  //       return { ...prev, name: value, slug: slugify(value) };
  //     }

  //     if (key === "slug") {
  //       return { ...prev, slug: slugify(value) };
  //     }

  //     return { ...prev, [key]: value };
  //   });
  // };

  // const nameError = useMemo(() => {
  //   if (!wasSubmitted) return undefined;

  //   return validateWorkspaceName(formData.name) ?? undefined;
  // }, [formData.name, wasSubmitted]);

  // const validationSlugError = useMemo(() => {
  //   if (!wasSubmitted) return undefined;

  //   return validateWorkspaceSlug(formData.slug) ?? undefined;
  // }, [formData.slug, wasSubmitted]);

  // const organizationSizeError = useMemo(() => {
  //   if (!wasSubmitted) return undefined;
  //   if (!formData.organizationSize.trim()) return "This field is required";

  //   return undefined;
  // }, [formData.organizationSize, wasSubmitted]);

  // const isButtonDisabled = useMemo(() => {
  //   if (isSubmitting) return true;
  //   if (validateWorkspaceName(formData.name)) return true;
  //   if (validateWorkspaceSlug(formData.slug)) return true;
  //   if (!formData.organizationSize.trim()) return true;
  //   if (slugError) return true;

  //   return false;
  // }, [formData.name, formData.slug, formData.organizationSize, slugError, isSubmitting]);

  const completeSetup = async (workspaceId: string) => {
    if (!user) return;

    await updateUserProfile({ lastWorkspaceId: workspaceId });

    await fetchCurrentUserSettings();
  };

  const handleCreateWorkspace = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    setWasSubmitted(true);

    if (isSubmitting) return;

    const nextNameError = validateWorkspaceName(formData.name);
    const nextSlugError = validateWorkspaceSlug(formData.slug);

    if (nextNameError || nextSlugError || !formData.organizationSize.trim()) {
      return;
    }

    setIsSubmitting(true);

    try {
      const promise = (async () => {
        const slugCheck = await workspaceService.workspaceSlugCheck(formData.slug);
        if (slugCheck.available !== true || RESTRICTED_URLS.includes(formData.slug)) {
          setSlugError("Workspace URL is already taken.");
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
      })();
      toast.promise(promise, {
        loading: "Creating workspace...",
        success: "Workspace created successfully",
        error: () => <div>hi</div>,
      });
      await promise;
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Form className="flex flex-col gap-10" onSubmit={handleCreateWorkspace}>
      <CommonOnboardingHeader title="Create your workspace" description="All your work - unified." />
    </Form>
  );
};
