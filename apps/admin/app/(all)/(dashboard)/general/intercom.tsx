import type { FC } from "react";
import { useState } from "react";
import useSWR from "swr";
import { MessageSquare } from "lucide-react";
// heroui
import { Card, Switch } from "@heroui/react";
// hooks
import { useInstance } from "@/hooks/store/use-instance";
// types
import type { TFormattedInstanceConfiguration } from "@syncturtle/types";

interface IIntercomConfig {
  isTelemetryEnabled: boolean;
}

export const IntercomConfig: FC<IIntercomConfig> = (props) => {
  const { isTelemetryEnabled } = props;
  // store hooks
  const { instanceConfigurations, updateInstanceConfigurations, fetchInstanceConfigurations } = useInstance();
  // states
  const [isSubmitting, setIsSubmitting] = useState(false);
  // derived values
  const isIntercomEnabled = isTelemetryEnabled
    ? instanceConfigurations
      ? instanceConfigurations.find((config) => config.key === "IS_INTERCOM_ENABLED")?.value === "1"
        ? true
        : false
      : undefined
    : false;

  const { isLoading } = useSWR(isTelemetryEnabled ? "INSTANCE_CONFIGURATIONS" : null, () =>
    isTelemetryEnabled ? fetchInstanceConfigurations() : null
  );

  const intialLoader = isLoading && isIntercomEnabled === undefined;

  const submitInstanceConfigurations = async (payload: Partial<TFormattedInstanceConfiguration>) => {
    try {
      await updateInstanceConfigurations(payload);
    } catch (error) {
      console.error(error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const enableIntercomConfig = () => {
    submitInstanceConfigurations({ IS_INTERCOM_ENABLED: isIntercomEnabled ? "0" : "1" });
  };

  return (
    <Card className="flex flex-row items-center">
      <div className="flex items-center justify-center w-10 h-10 bg-custom-background-80 rounded-full">
        <MessageSquare className="w-6 h-6" />
      </div>
      <div className="flex items-center justify-between w-full">
        <Card.Header>
          <Card.Title>Chat with us</Card.Title>
          <Card.Description>
            Let your users chat with us via Intercom or another service. Toggling Telemetry off turns this off
            automatically.
          </Card.Description>
        </Card.Header>
        <Switch
          aria-label="Enable telemetry"
          size="sm"
          isSelected={isIntercomEnabled ? true : false}
          onChange={enableIntercomConfig}
          isDisabled={!isTelemetryEnabled || isSubmitting || intialLoader}
        >
          <Switch.Control>
            <Switch.Thumb />
          </Switch.Control>
        </Switch>
      </div>
    </Card>
  );
};
