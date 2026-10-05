"use client";

import { useRef, useState } from "react";
import type { SyntheticEvent } from "react";
// heroui
import { Button, FieldError, Form, Input, Label, TextField } from "@heroui/react";
// syncturtle imports
import { RESTRICTED_URLS } from "@syncturtle/constants";
import type { IUser, IWorkspace } from "@syncturtle/types";
// hooks
import { useWorkspace } from "@/hooks/store/use-workspace";
// components
import { CommonOnboardingHeader } from "../common";
// services
import { WorkspaceService } from "@/services/workspace.service";
import { getPublicErrorMessage } from "@/helpers/error.helper";

const slugBase = (name: string) =>
  name
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 36)
    .replace(/-+$/g, "") || "space";

// Slug availability is checked before POST; the server still arbitrates races.
async function availableSlug(name: string): Promise<string> {
  const base = slugBase(name);
  for (let attempt = 0; attempt < 5; attempt++) {
    const candidate = `${base}-${crypto.randomUUID().slice(0, 8)}`;
    if (!RESTRICTED_URLS.includes(candidate) && (await workspaceService.workspaceSlugCheck(candidate)).available)
      return candidate;
  }
  throw new Error("Could not find an available space URL. Please try again.");
}

type Props = {
  user: IUser | undefined;
  onComplete: (workspace: IWorkspace) => Promise<void>;
  onBusyChange?: (busy: boolean) => void;
};

const workspaceService = new WorkspaceService();

export const WorkspaceCreateStep = ({ user, onComplete, onBusyChange }: Props) => {
  const { createWorkspace } = useWorkspace();
  const [name, setName] = useState("Personal");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string>();
  const created = useRef<IWorkspace | null>(null);
  const submitting = useRef(false);
  const nameError = !name.trim()
    ? "Enter a space name."
    : name.trim().length > 80
      ? "Limit your space name to 80 characters."
      : undefined;

  const submit = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitting.current || !user || nameError) return;
    submitting.current = true;
    setIsSubmitting(true);
    onBusyChange?.(true);
    setError(undefined);
    try {
      if (!created.current) {
        const slug = await availableSlug(name);
        // Store upsert completes before this resolves. No list/settings projection
        // refresh may replace the returned workspace or gate its availability.
        created.current = await createWorkspace({ name: name.trim(), slug });
      }
      // A failed profile/settings/completion retry reuses the successful POST.
      await onComplete(created.current);
    } catch (failure) {
      setError(getPublicErrorMessage(failure, "Could not prepare your space. Please try again."));
    } finally {
      submitting.current = false;
      setIsSubmitting(false);
      onBusyChange?.(false);
    }
  };

  return (
    <Form className="flex flex-col gap-6" onSubmit={submit} validationBehavior="aria">
      <CommonOnboardingHeader
        title="Create a space for what matters to you"
        description="Start with Personal or choose a name. You can use it on your own."
      />
      <TextField
        id="space-name"
        name="name"
        isRequired
        value={name}
        isInvalid={Boolean(nameError)}
        isDisabled={isSubmitting || Boolean(created.current)}
        onChange={setName}
      >
        <Label>Space name</Label>
        <Input maxLength={80} placeholder="Personal" />
        {nameError && <FieldError>{nameError}</FieldError>}
      </TextField>
      {error && <p role="alert">{error}</p>}
      <Button type="submit" isDisabled={isSubmitting || !user || Boolean(nameError)} isPending={isSubmitting}>
        {created.current ? "Continue with created space" : "Create space"}
      </Button>
    </Form>
  );
};
