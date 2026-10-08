import { SessionManagementPanel } from "@/components/sessions";

export default function SessionsPage() {
  return (
    <div className="relative container mx-auto flex h-full w-full flex-col space-y-6 p-4">
      <div className="mx-4 flex shrink-0 flex-col space-y-1 border-b border-custom-border-100 py-4">
        <h1 className="text-xl font-medium text-custom-text-100">Active sessions</h1>
        <p className="text-sm font-normal text-custom-text-300">
          Review and revoke the sessions that can access this account.
        </p>
      </div>
      <SessionManagementPanel />
    </div>
  );
}
