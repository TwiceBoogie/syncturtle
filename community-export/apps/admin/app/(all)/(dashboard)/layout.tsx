"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
// heroui
import { Spinner } from "@heroui/react";
// components
import { NewUserPopup } from "@/components/new-user-popup";
// store hooks
import { useUser } from "@/hooks/store/use-user";
// local components
import { AdminSidebar } from "./sidebar";
import { AdminHeader } from "./header";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  // store hooks
  const { isUserLoggedIn } = useUser();

  useEffect(() => {
    if (isUserLoggedIn === false) {
      router.push("/");
    }
  }, [router, isUserLoggedIn]);

  if (isUserLoggedIn === undefined) {
    return (
      <div className="relative flex h-screen w-full items-center justify-center">
        <Spinner />
      </div>
    );
  }

  if (isUserLoggedIn) {
    return (
      <div className="relative flex h-screen w-screen overflow-hidden">
        <AdminSidebar />
        <main className="relative flex h-full w-full flex-col overflow-hidden bg-custom-background-100">
          <AdminHeader />
          <div className="h-full w-full min-h-0 overflow-hidden">{children}</div>
        </main>
        <NewUserPopup />
      </div>
    );
  }

  return <></>;
}
