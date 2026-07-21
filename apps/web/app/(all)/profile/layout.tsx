"use client";

import { ProfileLayoutSidebar } from "./sidebar";

export default function ProfileSettingsLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <div className="relative flex h-full w-full overflow-hidden rounded-lg border border-custom-border-200">
        <ProfileLayoutSidebar />
        <main className="relative flex h-full w-full flex-col overflow-hidden bg-custom-background-100">
          <div className="h-full w-full overflow-hidden">{children}</div>
        </main>
      </div>
    </>
  );
}
