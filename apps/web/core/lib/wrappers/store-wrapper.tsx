import { useAppTheme } from "@/hooks/store/use-app-theme";
import { useRouterParams } from "@/hooks/store/use-router-params";
// import { useTranslation } from "@syncturtle/i18n";
import { useTheme } from "next-themes";
import { useParams } from "next/navigation";
import { FC, ReactNode, useEffect } from "react";

interface IStoreWrapper {
  children: ReactNode;
}

const StoreWrapper: FC<IStoreWrapper> = (props) => {
  const { children } = props;
  const { setTheme } = useTheme();
  const params = useParams();
  //store hooks
  const { setQuery } = useRouterParams();
  const { sidebarCollapsed, toggleSidebar } = useAppTheme();
  //   const { data: userProfile } = useUserProfile();
  // const { changeLanguage } = useTranslation();

  useEffect(() => {
    const localValue = localStorage && localStorage.getItem("app_sidebar_collapsed");
    const localBoolValue = localValue ? (localValue === "true" ? true : false) : false;
    if (localValue && sidebarCollapsed === undefined) toggleSidebar(localBoolValue);
  }, [sidebarCollapsed, setTheme, toggleSidebar]);

  useEffect(() => {
    if (!params) return;
    setQuery(params);
  }, [params, setQuery]);

  return <>{children}</>;
};

export default StoreWrapper;
