"use client";

import { FC, FormEvent, useMemo, useRef, useState } from "react";
// heroui
import {
  Button,
  CloseButton,
  FieldError,
  FieldGroup,
  Fieldset,
  Form,
  InputGroup,
  Label,
  Spinner,
  TextField,
} from "@heroui/react";
// helpers
import { checkEmailValidity } from "@syncturtle/utils";
// hooks
import { useTranslation } from "@syncturtle/i18n";
// types
import { IEmailCheckData } from "@syncturtle/types";

interface IAuthEmailForm {
  defaultEmail: string;
  onSubmit: (data: IEmailCheckData) => Promise<void>;
}

export const AuthEmailForm: FC<IAuthEmailForm> = (props) => {
  const { defaultEmail, onSubmit } = props;
  // states
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [email, setEmail] = useState(defaultEmail);
  const [isFocused, setIsFocused] = useState(true);
  // ref
  const inputRef = useRef<HTMLInputElement>(null);
  // i18n hook
  const { t } = useTranslation();

  const emailError = useMemo(
    () => (email && !checkEmailValidity(email) ? { email: "auth.common.email.errors.invalid" } : undefined),
    [email]
  );

  const handleFormSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    const payload: IEmailCheckData = {
      email: email,
    };
    try {
      await onSubmit(payload);
    } finally {
      setIsSubmitting(false);
    }
  };
  const clearEmail = () => {
    setEmail("");
    inputRef.current?.focus();
  };

  const showEmailError = Boolean(emailError?.email) && !isFocused;
  const isButtonDisabled = email.length === 0 || Boolean(emailError?.email) || isSubmitting;

  return (
    <Form onSubmit={handleFormSubmit}>
      <Fieldset>
        <FieldGroup>
          <TextField
            isRequired
            name="email"
            type="email"
            isInvalid={showEmailError}
            onChange={(value: string) => setEmail(value)}
            value={email}
            onFocus={() => {
              setIsFocused(true);
            }}
            onBlur={() => {
              setIsFocused(false);
            }}
          >
            <Label>{t("auth.common.email.label")}</Label>
            <InputGroup>
              <InputGroup.Input placeholder={t("auth.common.email.placeholder")} ref={inputRef} />
              {email.length > 0 && (
                <InputGroup.Suffix>
                  <CloseButton onPress={clearEmail} />
                </InputGroup.Suffix>
              )}
            </InputGroup>
            {emailError?.email && !isFocused && <FieldError>{t(emailError.email)}</FieldError>}
          </TextField>
        </FieldGroup>
        <Fieldset.Actions>
          <Button type="submit" isDisabled={isButtonDisabled} isPending={isSubmitting}>
            {({ isPending }) => (
              <>
                {isPending ? <Spinner /> : null}
                {t("common.continue")}
              </>
            )}
          </Button>
        </Fieldset.Actions>
      </Fieldset>
    </Form>
  );
};
