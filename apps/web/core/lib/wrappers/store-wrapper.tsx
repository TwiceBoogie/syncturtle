import type { FC, ReactNode } from "react";
import { useEffect } from "react";
import { useParams } from "next/navigation";
import { useTranslation } from "@syncturtle/i18n";
import type { TLanguage } from "@syncturtle/i18n";
// hooks
import { useAppTheme } from "@/hooks/store/use-app-theme";
import { useRouterParams } from "@/hooks/store/use-router-params";
import { useUserProfile } from "@/hooks/store/user";

interface IStoreWrapper {
  children: ReactNode;
}

const StoreWrapper: FC<IStoreWrapper> = (props) => {
  const { children } = props;

  const params = useParams();
  //store hooks
  const { setQuery } = useRouterParams();
  const { hydrateSidebarCollapsed } = useAppTheme();
  const { data: userProfile } = useUserProfile();
  const { changeLanguage } = useTranslation();

  useEffect(() => {
    hydrateSidebarCollapsed();
  }, [hydrateSidebarCollapsed]);

  useEffect(() => {
    if (!userProfile?.language) return;
    changeLanguage(userProfile?.language as TLanguage);
  }, [userProfile?.language, changeLanguage]);

  useEffect(() => {
    if (!params) return;
    setQuery(params);
  }, [params, setQuery]);

  return <>{children}</>;
};

export default StoreWrapper;
