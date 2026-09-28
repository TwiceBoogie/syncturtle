import { Button, Chip, Spinner } from "@heroui/react";
import type { IUserSession } from "@syncturtle/types";

interface ISessionCardProps {
  session: IUserSession;
  isMutationPending: boolean;
  isActionDisabled: boolean;
  onRequestRevoke: (session: IUserSession) => void;
}

const timestampFormatter = new Intl.DateTimeFormat("en-US", {
  year: "numeric",
  month: "short",
  day: "numeric",
  hour: "numeric",
  minute: "2-digit",
  timeZone: "UTC",
  timeZoneName: "short",
});

const formatTimestamp = (value: string): string => {
  if (!value.trim()) return "Unavailable";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Unavailable";
  return timestampFormatter.format(date);
};

const Timestamp = ({ label, value }: { label: string; value: string }) => {
  const formatted = formatTimestamp(value);
  return (
    <div>
      <dt className="text-xs font-medium text-custom-text-300">{label}</dt>
      <dd className="mt-1 text-sm text-custom-text-100">
        {formatted === "Unavailable" ? formatted : <time dateTime={value}>{formatted}</time>}
      </dd>
    </div>
  );
};

export const SessionCard = ({ session, isMutationPending, isActionDisabled, onRequestRevoke }: ISessionCardProps) => (
  <article
    aria-label={session.current ? "Current session" : "Active session"}
    className="rounded-lg border border-custom-border-100 bg-custom-background-100 p-4"
  >
    <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
      <div className="min-w-0 space-y-3">
        <div className="flex flex-wrap items-center gap-2">
          <h2 className="text-base font-medium text-custom-text-100">Session</h2>
          {session.current && (
            <Chip color="accent" size="sm" variant="soft">
              Current session
            </Chip>
          )}
          {session.administrator && (
            <Chip color="default" size="sm" variant="soft">
              Administrator session
            </Chip>
          )}
        </div>
        <dl className="grid grid-cols-1 gap-x-8 gap-y-4 sm:grid-cols-2 xl:grid-cols-4">
          <Timestamp label="Created" value={session.createdAt} />
          <Timestamp label="Last used" value={session.lastUsedAt} />
          <Timestamp label="Idle expiration" value={session.idleExpiresAt} />
          <Timestamp label="Absolute expiration" value={session.absoluteExpiresAt} />
        </dl>
      </div>
      <Button
        className="w-full shrink-0 sm:w-auto"
        variant="danger-soft"
        isDisabled={isActionDisabled}
        onPress={() => onRequestRevoke(session)}
      >
        {isMutationPending ? <Spinner aria-label="Revoking session" color="current" size="sm" /> : null}
        {isMutationPending ? "Revoking..." : session.current ? "Revoke current session" : "Revoke session"}
      </Button>
    </div>
  </article>
);
