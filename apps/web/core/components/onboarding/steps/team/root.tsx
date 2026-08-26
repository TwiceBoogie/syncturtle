"use client";

import type { FC, SyntheticEvent } from "react";
import { useState, useMemo } from "react";
// heroui
import type { Key } from "@heroui/react";
import { Button, FieldError, Form, Input, Label, ListBox, Select, Spinner, TextField, toast } from "@heroui/react";
import { Plus, XCircle } from "lucide-react";
// syncturtle imports
import { EOnboardingSteps, EUserPermissions, ROLE_OPTIONS } from "@syncturtle/constants";
import { useTranslation } from "@syncturtle/i18n";
import type { IWorkspaceBulkInviteFormData, TOnboardingStep, TUserPermissions } from "@syncturtle/types";
// store hooks
import { useWorkspace } from "@/hooks/store/use-workspace";
// components
import { CommonOnboardingHeader } from "../common";
// services
import { WorkspaceService } from "@/services/workspace.service";
// helpers
import { getPublicErrorMessage } from "@/helpers/error.helper";

interface IInviteTeamStep {
  handleStepChange: (step: TOnboardingStep, skipInvites?: boolean) => void;
}

interface IEmailRole {
  id: string;
  email: string;
  role: TUserPermissions;
  roleActive: boolean;
}

const emailRegex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;

const placeholderEmails = [
  "charlie.taylor@frstflt.com",
  "octave.chanute@frstflt.com",
  "george.spratt@frstflt.com",
  "frank.coffyn@frstflt.com",
  "amos.root@frstflt.com",
  "edward.deeds@frstflt.com",
  "charles.m.manly@frstflt.com",
  "glenn.curtiss@frstflt.com",
  "thomas.selfridge@frstflt.com",
  "albert.zahm@frstflt.com",
];

const createInviteRow = (index: number): IEmailRole => ({
  id: `invite-row-${index}`,
  email: "",
  role: EUserPermissions.MEMBER,
  roleActive: false,
});

const getEmailError = (email: string): string | undefined => {
  const trimmedEmail = email.trim();

  if (!trimmedEmail) return undefined;

  if (!emailRegex.test(trimmedEmail)) {
    return "That doesn't look like an email address";
  }

  return undefined;
};

const workspaceService = new WorkspaceService();

export const InviteTeamStep: FC<IInviteTeamStep> = (props) => {
  const { handleStepChange } = props;
  // hooks
  const { t } = useTranslation();
  const { workspaces } = useWorkspace();
  // states
  const [nextId, setNextId] = useState(3);
  const [members, setMembers] = useState<IEmailRole[]>([createInviteRow(0), createInviteRow(1), createInviteRow(2)]);
  const [wasSubmitted, setWasSubmitted] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const nextStep = async () => {
    await handleStepChange(EOnboardingSteps.INVITE_MEMBERS);
  };

  const updateMemberEmail = (index: number, value: string) => {
    setMembers((prevMembers) =>
      prevMembers.map((member, memberIndex) => {
        if (memberIndex !== index) return member;

        return {
          ...member,
          email: value,
          roleActive: value.trim().length > 0,
        };
      })
    );
  };

  const isUserPermission = (value: number): value is TUserPermissions =>
    value === EUserPermissions.GUEST || value === EUserPermissions.MEMBER || value === EUserPermissions.ADMIN;

  const updateMemberRole = (index: number, value: Key | Key[] | null) => {
    if (value === null || Array.isArray(value)) return;

    const nextRole = Number(value);

    if (!isUserPermission(nextRole)) return;

    setMembers((prevMembers) =>
      prevMembers.map((member, memberIndex) => {
        if (memberIndex !== index) return member;

        return {
          ...member,
          role: nextRole,
          roleActive: true,
        };
      })
    );
  };

  const removeMember = (index: number) => {
    setMembers((prevMembers) => prevMembers.filter((_, memberIndex) => memberIndex !== index));
  };

  const appendMember = () => {
    setMembers((prevMembers) => [...prevMembers, createInviteRow(nextId)]);
    setNextId((prevNextId) => prevNextId + 1);
  };

  const onSubmit = async (event: SyntheticEvent<HTMLFormElement>) => {
    event.preventDefault();
    setWasSubmitted(true);

    if (!workspace || isSubmitDisabled) return;

    const payload: IWorkspaceBulkInviteFormData = {
      emails: filledMembers.map((member) => ({
        email: member.email.trim(),
        role: member.role,
      })),
    };

    setIsSubmitting(true);

    const invitePromise = workspaceService.inviteWorkspace(workspace.slug, payload).then(async () => {
      await nextStep();
    });

    toast.promise(invitePromise, {
      loading: "Sending invitations...",
      success: "Invitations sent successfully.",
      error: (error) => getPublicErrorMessage(error, "Could not send invitations."),
    });

    try {
      await invitePromise;
    } finally {
      setIsSubmitting(false);
    }
  };

  // derived values
  const workspacesList = Object.values(workspaces ?? {});
  const workspace = workspacesList[0];
  const filledMembers = useMemo(() => members.filter((member) => member.email.trim().length > 0), [members]);
  const hasValidEmail = useMemo(
    () => filledMembers.some((member) => emailRegex.test(member.email.trim())),
    [filledMembers]
  );
  const hasInvalidEmail = useMemo(
    () => filledMembers.some((member) => !emailRegex.test(member.email.trim())),
    [filledMembers]
  );
  const isSubmitDisabled = isSubmitting || !hasValidEmail || hasInvalidEmail || !workspace;

  return (
    <Form aria-label="Invite teammates" validationBehavior="aria" onSubmit={onSubmit}>
      <CommonOnboardingHeader
        title="Invite your teammates"
        description="Work in Syncturtle happens best with your team. Invite them now to use Syncturtle to its potential"
      />

      <div className="w-full py-4 text-sm">
        <div className="group relative mx-8 grid grid-cols-10 gap-4 py-2">
          <div className="col-span-6 px-1 text-sm font-medium text-custom-text-200">Email</div>
          <div className="col-span-4 px-1 text-sm font-medium text-custom-text-200">Role</div>
        </div>

        <div className="mb-3 space-y-3 sm:space-y-4">
          {members.map((member, index) => {
            const emailError = getEmailError(member.email);
            const shouldShowEmailError = Boolean(emailError) && wasSubmitted;

            return (
              <div key={member.id}>
                <div className="group relative grid grid-cols-10 gap-4">
                  <div className="col-span-6">
                    <TextField
                      id={`emails.${index}.email`}
                      name={`emails.${index}.email`}
                      value={member.email}
                      onChange={(value: string) => updateMemberEmail(index, value)}
                      isInvalid={shouldShowEmailError}
                      variant="secondary"
                    >
                      <Label className="sr-only">Email address {index + 1}</Label>
                      <Input
                        type="email"
                        autoComplete="off"
                        placeholder={placeholderEmails[index % placeholderEmails.length]}
                      />
                      {shouldShowEmailError && <FieldError>{emailError}</FieldError>}
                    </TextField>
                  </div>

                  <div className="col-span-4 mr-8">
                    <Select
                      fullWidth
                      variant="secondary"
                      placeholder="Select role"
                      value={String(member.role)}
                      name={`emails.${index}.role`}
                      aria-label={`Role for invite ${index + 1}`}
                      onChange={(value) => updateMemberRole(index, value)}
                    >
                      <Select.Trigger>
                        <Select.Value />
                        <Select.Indicator />
                      </Select.Trigger>
                      <Select.Popover>
                        <ListBox>
                          {ROLE_OPTIONS.map((role) => (
                            <ListBox.Item key={role.value} id={String(role.value)} textValue={t(role.i18nTitle)}>
                              <div>
                                <div className="text-sm font-medium">{t(role.i18nTitle)}</div>
                                {/* <div className="flex text-xs text-custom-text-300">{t(role.i18nDescription)}</div> */}
                              </div>
                              <ListBox.ItemIndicator />
                            </ListBox.Item>
                          ))}
                        </ListBox>
                      </Select.Popover>
                    </Select>
                  </div>
                  {members.length > 1 && (
                    <button
                      type="button"
                      aria-label={`Remove invite row ${index + 1}`}
                      className="absolute right-0 place-items-center self-center rounded group-hover:grid"
                      onClick={() => removeMember(index)}
                    >
                      <XCircle className="h-5 w-5 pl-0.5 text-custom-text-400" />
                    </button>
                  )}
                </div>
                {member.email && emailError && !wasSubmitted && (
                  <div className="mx-8 my-1">
                    <span className="mt-1 text-xs text-red-500">{emailError}</span>
                  </div>
                )}
              </div>
            );
          })}
        </div>
        <button
          type="button"
          className="mx-8 flex items-center gap-1.5 bg-transparent text-sm font-medium text-custom-primary-100 outline-custom-primary-100"
          onClick={appendMember}
        >
          <Plus className="h-4 w-4" strokeWidth={2} />
          Add another
        </button>
      </div>
      <div className="mx-auto flex w-full flex-col items-center justify-center gap-4 px-8 sm:px-2">
        <Button
          variant="primary"
          type="submit"
          size="sm"
          fullWidth
          isDisabled={isSubmitDisabled}
          isPending={isSubmitting}
        >
          {({ isPending }) => (isPending ? <Spinner color="current" size="sm" /> : "Continue")}
        </Button>

        <Button variant="secondary" type="button" size="sm" fullWidth onPress={nextStep} isDisabled={isSubmitting}>
          I&apos;ll do it later
        </Button>
      </div>
    </Form>
  );
};
