"use client";

import type { FC } from "react";
import { useCallback, useMemo, useState } from "react";
// heroui
import { Button, Description, Disclosure, FieldError, InputGroup, Label, ProgressBar, TextField } from "@heroui/react";
import { Check, Eye, EyeOff, Lock } from "lucide-react";
import { cn } from "@syncturtle/ui";

type TPasswordStrengthColor = "default" | "accent" | "success" | "warning" | "danger";

type TPasswordStrength = {
  score: number;
  label: string;
  color: TPasswordStrengthColor;
};

interface IPasswordState {
  password: string;
  confirmPassword: string;
}

interface ISetPasswordRootProps {
  onPasswordChange?: (password: string) => void;
  onConfirmPasswordChange?: (confirmPassword: string) => void;
  disabled?: boolean;
}

const defaultPasswordState: IPasswordState = {
  password: "",
  confirmPassword: "",
};

const getPasswordStrength = (password: string): TPasswordStrength => {
  if (!password) {
    return {
      score: 0,
      label: "Enter a password",
      color: "default",
    };
  }

  let score = 0;

  if (password.length >= 8) score += 1;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score += 1;
  if (/\d/.test(password)) score += 1;
  if (/[^A-Za-z0-9]/.test(password)) score += 1;

  if (score <= 1) {
    return {
      score,
      label: "Weak password",
      color: "danger",
    };
  }

  if (score <= 3) {
    return {
      score,
      label: "Good password",
      color: "warning",
    };
  }

  return {
    score,
    label: "Strong password",
    color: "success",
  };
};

export const SetPasswordRoot: FC<ISetPasswordRootProps> = (props) => {
  const { onPasswordChange, onConfirmPasswordChange, disabled = false } = props;
  // states
  const [isExpanded, setIsExpanded] = useState(false);
  const [passwordState, setPasswordState] = useState<IPasswordState>(defaultPasswordState);
  const [showPassword, setShowPassword] = useState({
    password: false,
    confirmPassword: false,
  });

  const handlePasswordChange = useCallback(
    (field: keyof IPasswordState, value: string) => {
      setPasswordState((prev) => ({ ...prev, [field]: value }));

      if (field === "password") {
        onPasswordChange?.(value);
      }

      if (field === "confirmPassword") {
        onConfirmPasswordChange?.(value);
      }
    },
    [onPasswordChange, onConfirmPasswordChange]
  );

  const handleShowPassword = (key: keyof typeof showPassword) =>
    setShowPassword((prev) => ({ ...prev, [key]: !prev[key] }));

  const strength = useMemo(() => getPasswordStrength(passwordState.password), [passwordState.password]);

  const isPasswordValid = useMemo(() => {
    const { password, confirmPassword } = passwordState;
    return password.length >= 8 && password === confirmPassword;
  }, [passwordState]);

  const hasPasswordMismatch = useMemo(() => {
    const { password, confirmPassword } = passwordState;
    return confirmPassword.length > 0 && password !== confirmPassword;
  }, [passwordState]);

  return (
    <Disclosure
      isExpanded={isExpanded}
      onExpandedChange={(nextIsExpanded) => {
        if (disabled) return;
        setIsExpanded(nextIsExpanded);
      }}
      isDisabled={disabled}
      className="overflow-hidden rounded-lg bg-custom-background-90 transition-all duration-300 ease-in-out"
    >
      <Disclosure.Heading>
        <Disclosure.Trigger
          className={cn(
            "flex w-full items-center justify-between px-3 py-2 text-sm transition-colors duration-200",
            disabled ? "cursor-not-allowed opacity-50" : "cursor-pointer",
            isExpanded && "pb-1"
          )}
        >
          <div className="flex items-center gap-1 text-custom-text-300">
            <Lock className="size-3" />
            <span className="font-medium">Set a password</span>
            <span>(Optional)</span>
          </div>

          <Disclosure.Indicator
            className={cn(
              "size-4 text-custom-text-400 transition-transform duration-300 ease-in-out",
              isExpanded && "rotate-180"
            )}
          />
        </Disclosure.Trigger>
      </Disclosure.Heading>

      <Disclosure.Content>
        <Disclosure.Body className="flex flex-col gap-4 pb-2 pt-1">
          <TextField
            name="password"
            type={showPassword.password ? "text" : "password"}
            value={passwordState.password}
            isDisabled={disabled}
            onChange={(value: string) => handlePasswordChange("password", value)}
            validate={(value: string) => {
              if (!value) return null;
              if (value.length < 8) {
                return "Password must be at least 8 characters or more";
              }
              return null;
            }}
          >
            <Label>Password</Label>
            <InputGroup variant="secondary">
              <InputGroup.Input placeholder="Set a password" />
              <InputGroup.Suffix>
                <Button
                  isIconOnly
                  type="button"
                  size="sm"
                  variant="ghost"
                  aria-label={showPassword.password ? "Hide password" : "Show password"}
                  isDisabled={disabled}
                  onPress={() => handleShowPassword("password")}
                  onMouseDown={(event) => event.preventDefault()}
                >
                  {showPassword.password ? <Eye /> : <EyeOff />}
                </Button>
              </InputGroup.Suffix>
            </InputGroup>
            {passwordState.password.length > 0 && (
              <Description className="mt-1 flex flex-col gap-2">
                <ProgressBar
                  aria-label="Password strength"
                  value={strength.score}
                  minValue={0}
                  maxValue={4}
                  color={strength.color}
                  size="sm"
                >
                  <ProgressBar.Track>
                    <ProgressBar.Fill />
                  </ProgressBar.Track>
                </ProgressBar>
                {strength.label}
              </Description>
            )}
            <FieldError />
          </TextField>

          <TextField
            name="confirmPassword"
            type={showPassword.confirmPassword ? "text" : "password"}
            value={passwordState.confirmPassword}
            isDisabled={disabled}
            validate={(value: string) => {
              if (!passwordState.password && !value) return null;

              if (passwordState.password !== value) {
                return "Passwords do not match";
              }

              return null;
            }}
            onChange={(value: string) => handlePasswordChange("confirmPassword", value)}
          >
            <Label>Confirm password</Label>
            <InputGroup variant="secondary">
              <InputGroup.Input placeholder="Confirm password" />
              <InputGroup.Suffix>
                <Button
                  isIconOnly
                  type="button"
                  size="sm"
                  variant="ghost"
                  aria-label={showPassword.confirmPassword ? "Hide confirm password" : "Show confirm password"}
                  isDisabled={disabled}
                  onPress={() => handleShowPassword("confirmPassword")}
                  onMouseDown={(event) => event.preventDefault()}
                >
                  {showPassword.confirmPassword ? <Eye /> : <EyeOff />}
                </Button>
              </InputGroup.Suffix>
            </InputGroup>
            <Description>
              {isPasswordValid && (
                <p className="flex items-center gap-1 text-xs text-green-500">
                  <Check className="size-3" />
                  Passwords match
                </p>
              )}
            </Description>
            <FieldError />
          </TextField>
          {hasPasswordMismatch && <p className="text-xs text-red-500">Passwords do not match</p>}
        </Disclosure.Body>
      </Disclosure.Content>
    </Disclosure>
  );
};
