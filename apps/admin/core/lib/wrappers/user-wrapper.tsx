"use client";

import { FC, ReactNode, useEffect } from "react";
// hooks
import { useAppTheme } from "@/hooks/store/use-app-theme";
import { useUser } from "@/hooks/store/use-user";
import { useInstance } from "@/hooks/store/use-instance";
import useSWR from "swr";

interface IUserWrapperProps {
  children: ReactNode;
}

export const UserWrapper: FC<IUserWrapperProps> = (props) => {
  const { children } = props;
  // hooks
  const { isSidebarCollapsed, toggleSidebar } = useAppTheme();
  const { currentUser, fetchCurrentUser } = useUser();
  const { fetchInstanceAdmins } = useInstance();

  useSWR("CURRENT_USER", () => fetchCurrentUser(), {
    shouldRetryOnError: false,
  });

  useSWR("INSTANCE_ADMINS", () => fetchInstanceAdmins());

  useEffect(() => {
    const localValue = localStorage && localStorage.getItem("god_mode_sidebar_collapsed");
    const localBoolValue = localValue ? (localValue === "true" ? true : false) : false;
    if (isSidebarCollapsed === undefined && localBoolValue != isSidebarCollapsed) toggleSidebar(localBoolValue);
  }, [isSidebarCollapsed, currentUser, toggleSidebar]);

  return <>{children}</>;
};
