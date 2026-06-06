"use client";

import { FormEvent, useEffect, useMemo, useRef, useState } from "react";
import { useRouter } from "next/navigation";
// heroui
import {
  FieldError,
  Form,
  Input,
  InputGroup,
  Label,
  TextField,
  Select,
  ListBox,
  Key,
  Button,
  toast,
} from "@heroui/react";
// constants
import { ORGANIZATION_SIZE, WEBSITE_URL } from "@syncturtle/constants";
// types
import { IWorkspace, TFieldErrors, TSlugStatus } from "@syncturtle/types";
import { WorkspaceService } from "@/services/workspace.service";
import { useDebouncerValue } from "@syncturtle/hooks";
import { useWorkspace } from "@/hooks/store/use-workspace";
import { getFieldErrors, getPublicErrorMessage } from "@/helpers/error.helper";

const workspaceService = new WorkspaceService();

function validateSlugLocal(slug: string): string | null {
  if (!slug) return null;
  if (slug.length > 48) return "Limit your URL to 48 characters.";
  if (slug.length < 3) return "Slug must be at least 3 characters.";
  return null;
}

export const WorkspaceCreateForm = () => {
  // router
  const router = useRouter();
  // states
  const [formData, setFormData] = useState<Partial<IWorkspace>>({
    name: "",
    slug: "",
    organizationSize: "",
  });
  const [serverErrors, setServerErrors] = useState<TFieldErrors>({});
  // async state
  const [slugStatus, setSlugStatus] = useState<TSlugStatus>("idle");
  const [slugMessage, setSlugMessage] = useState<string | undefined>(undefined);
  // store hooks
  const { createWorkspace } = useWorkspace();

  const clearServerError = (key: keyof IWorkspace) => {
    setServerErrors((prev) => {
      if (!prev[key]) return prev;

      const next = { ...prev };
      delete next[key];
      return next;
    });
  };

  const handleFormChange = (key: keyof IWorkspace, value: string) => {
    clearServerError(key);
    setFormData((prev) => ({ ...prev, [key]: value }));
  };

  const submit = async (data: Partial<IWorkspace>) => {
    setServerErrors({});

    try {
      await createWorkspace(data);

      toast("Success", {
        description: "Workspace created successfully",
        actionProps: {
          children: "Dismiss",
          onPress: () => toast.clear(),
          className: "bg-success text-success-foreground",
          variant: "tertiary",
        },
      });
    } catch (error: unknown) {
      const fieldErrors = getFieldErrors(error);
      setServerErrors(fieldErrors);

      toast("Error!", {
        description: getPublicErrorMessage(error, "Failed to create workspace."),
        actionProps: {
          children: "Dismiss",
          onPress: () => toast.clear(),
          variant: "danger",
        },
      });

      throw error;
    }
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    try {
      await submit(formData);
    } catch (error) {
      console.error(error);
    }
  };

  const slug = formData.slug ?? "";
  const debouncedSlug = useDebouncerValue(slug.trim(), 650);

  // abort + "latest request wins"
  const abortRef = useRef<AbortController | null>(null);
  const reqIdRef = useRef(0);

  // local validation (sync) to gate api call
  const localSlugError = useMemo(() => validateSlugLocal(debouncedSlug), [debouncedSlug]);

  useEffect(() => {
    // if empty: reset
    if (!debouncedSlug || localSlugError) {
      abortRef.current?.abort();
      return;
    }

    // start new request: cancel previous
    abortRef.current?.abort();
    const controller = new AbortController();
    abortRef.current = controller;

    const reqId = ++reqIdRef.current;

    (async () => {
      setSlugStatus("checking");
      setSlugMessage(undefined);
      try {
        const result = await workspaceService.slugCheck(debouncedSlug, controller.signal);

        // ignore stale
        if (reqId !== reqIdRef.current) return;
        if (result.available) {
          setSlugStatus("available");
          setSlugMessage(result.message ?? "Available");
        } else {
          setSlugStatus("unavailable");
          setSlugMessage(result.message ?? "That URL is taken.");
        }
      } catch (error: unknown) {
        if (error instanceof DOMException && error.name === "AbortError") return;

        if (reqId !== reqIdRef.current) return;
        setSlugStatus("idle");
        setSlugMessage("Couldn't verify slug right now.");
      }
    })();

    return () => controller.abort();
  }, [debouncedSlug, localSlugError]);

  // derived states
  const isChecking = slugStatus === "checking";
  const isAvailable = slugStatus === "available";
  const isUnavailable = slugStatus === "unavailable";

  return (
    <Form className="space-y-6" onSubmit={handleSubmit} validationErrors={serverErrors}>
      <div className="grid grid-cols-1 w-full max-w-4xl gap-x-10 gap-y-6 lg:grid-cols-2">
        <TextField
          isRequired
          name="name"
          type="text"
          value={formData.name}
          onChange={(value: string) => handleFormChange("name", value)}
          validate={(value: string) => {
            if (!value) return null;
            if (!/^[\w\s-]*$/.test(value)) {
              return `Workspaces names can only contain (" "), ( - ), ( _ ) and alphanumric characters.`;
            }
            if (value.length > 80) {
              return "Limit your name to 80 characters";
            }
            return null;
          }}
        >
          <Label>Name your workspace</Label>
          <Input placeholder="name" />
          <FieldError />
        </TextField>
        <TextField
          isRequired
          name="slug"
          type="text"
          value={formData.slug}
          onChange={(value: string) => handleFormChange("slug", value)}
          validate={(value: string) => {
            const basic = validateSlugLocal(value.trim());
            if (basic) return basic;

            if (isUnavailable) return slugMessage ?? "That URL is taken.";
            return null;
          }}
        >
          <Label>Set your workspace&apos;s URL</Label>
          <InputGroup>
            <InputGroup.Prefix>{WEBSITE_URL}/</InputGroup.Prefix>
            <InputGroup.Input placeholder="example" />
            <InputGroup.Suffix>
              {isChecking ? (
                <span className="text-sm opacity-70">Checking...</span>
              ) : isAvailable ? (
                <span className="text-sm text-green-500">✓</span>
              ) : isUnavailable ? (
                <span className="text-sm text-red-500">✕</span>
              ) : null}
            </InputGroup.Suffix>
          </InputGroup>
          <FieldError />
        </TextField>
        <Select
          isRequired
          placeholder="Select a range"
          value={formData.organizationSize}
          onChange={(value: Key | null) =>
            handleFormChange("organizationSize", value === null ? "" : (value as string))
          }
        >
          <Label>How many people will use this workspace?</Label>
          <Select.Trigger>
            <Select.Value />
            <Select.Indicator />
          </Select.Trigger>
          <Select.Popover>
            <ListBox>
              {ORGANIZATION_SIZE.map((size) => (
                <ListBox.Item key={size} id={size} textValue={size}>
                  {size}
                  <ListBox.ItemIndicator />
                </ListBox.Item>
              ))}
            </ListBox>
          </Select.Popover>
        </Select>
      </div>
      <Button type="submit">Submit</Button>
    </Form>
  );
};
