"use client";

import DefaultLayout from "@/layouts/auth-layout";
import { MaintenanceMessage } from "@/syncturtle-web/components/instance";
// import { useTheme } from "next-themes";

export const MaintenanceView = () => (
  <DefaultLayout>
    <div className="relative">
      <div className="relative w-full">{/* image */}</div>
      <div className="w-full">
        <MaintenanceMessage />
      </div>
    </div>
  </DefaultLayout>
);
