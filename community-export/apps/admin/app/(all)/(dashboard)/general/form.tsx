"use client";

import { FC, FormEvent, useState } from "react";
// heroui
import {
  Button,
  Card,
  FieldGroup,
  Fieldset,
  Form,
  Input,
  Label,
  Spinner,
  Switch,
  TextField,
  toast,
} from "@heroui/react";
// components
import { IntercomConfig } from "./intercom";
// hooks
import { useInstance } from "@/hooks/store/use-instance";
// types
import { IInstance, IInstanceAdmin } from "@syncturtle/types";
import { Telescope } from "lucide-react";

interface IGeneralConfigurationForm {
  instance: IInstance;
  instanceAdmins: IInstanceAdmin[];
}

export const GeneralConfigurationForm: FC<IGeneralConfigurationForm> = (props) => {
  const { instance, instanceAdmins } = props;
  // store hooks
  const { instanceConfigurations, updateInstanceInfo, updateInstanceConfigurations } = useInstance();
  // states
  const [formData, setFormData] = useState<Partial<IInstance>>({
    instanceName: instance.instanceName,
    isTelemetryEnabled: instance.isTelemetryEnabled,
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleFormChange = (key: keyof typeof formData, value: string | boolean) =>
    setFormData((prev) => ({ ...prev, [key]: value }));

  const submit = async (data: Partial<IInstance>) => {
    const payload: Partial<IInstance> = { ...data };

    // update the intercom configuration
    const isIntercomEnabled =
      instanceConfigurations?.find((config) => config.key === "IS_INTERCOM_ENABLED")?.value === "1";
    if (!payload.isTelemetryEnabled && isIntercomEnabled) {
      await updateInstanceConfigurations({ IS_INTERCOM_ENABLED: "0" });
    }

    await updateInstanceInfo(payload).then(() =>
      toast("Success", {
        actionProps: {
          children: "Dismiss",
          onPress: () => toast.clear(),
          className: "bg-success text-success-foreground",
          variant: "tertiary",
        },
        description: "Settings updated successfully",
      })
    );
  };

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      await submit(formData);
    } catch (error) {
      console.error(error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Form onSubmit={handleSubmit}>
      <Fieldset>
        <Fieldset.Legend>Instance details</Fieldset.Legend>
        <FieldGroup>
          <div className="grid-col grid w-full grid-cols-1 items-center justify-between gap-4 md:grid-cols-2 lg:grid-cols-3">
            <TextField
              isRequired
              name="instanceName"
              type="text"
              value={formData.instanceName ?? ""}
              onChange={(value: string) => handleFormChange("instanceName", value)}
            >
              <Label>Name of instance</Label>
              <Input placeholder="Instance name" />
            </TextField>
            <TextField name="email" type="email" value={instanceAdmins[0].userDetail.email ?? ""} isDisabled>
              <Label>Email</Label>
              <Input placeholder="Admin email" />
            </TextField>
            <TextField name="instanceId" type="text" value={instance.instanceId} isDisabled>
              <Label>Instance ID</Label>
              <Input />
            </TextField>
          </div>
          <div className="flex flex-col space-y-2">
            <div className="text-lg font-medium">Chat + telemetry</div>
            <IntercomConfig isTelemetryEnabled={formData.isTelemetryEnabled ?? false} />
            <Card className="flex flex-row items-center">
              <div className="flex items-center justify-center w-10 h-10 bg-custom-background-80 rounded-full">
                <Telescope className="w-6 h-6" />
              </div>
              <div className="flex items-center justify-between w-full">
                <Card.Header>
                  <Card.Title>Let Syncturtle collect anonymous usage data</Card.Title>
                  <Card.Description>
                    No PII is collected. This anonymized data is used to understand how you use Syncturtle and build new
                    features in line with{" "}
                  </Card.Description>
                </Card.Header>
                <Switch
                  aria-label="Enable telemetry"
                  size="sm"
                  name="isTelemetryEnabled"
                  isSelected={formData.isTelemetryEnabled ?? false}
                  onChange={(isSelected: boolean) => handleFormChange("isTelemetryEnabled", isSelected)}
                  isDisabled={isSubmitting}
                >
                  <Switch.Control>
                    <Switch.Thumb />
                  </Switch.Control>
                </Switch>
              </div>
            </Card>
          </div>
        </FieldGroup>
        <Fieldset.Actions>
          <Button type="submit" isPending={isSubmitting}>
            {({ isPending }) => (
              <>
                {isPending ? <Spinner /> : null}
                {isPending ? "Submitting..." : "Submit"}
              </>
            )}
          </Button>
        </Fieldset.Actions>
      </Fieldset>
    </Form>
  );
};
