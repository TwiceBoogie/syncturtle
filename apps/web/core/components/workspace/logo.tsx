import type { FC } from "react";
// syncturtle imports
import { useTranslation } from "@syncturtle/i18n";
import { getFileURL } from "@syncturtle/utils";
import { cn } from "@syncturtle/ui";

interface IWorkspaceLogo {
  logo: string | null | undefined;
  name: string | undefined;
  classNames?: string;
}

export const WorkspaceLogo: FC<IWorkspaceLogo> = (props) => {
  const { logo, name, classNames } = props;

  const { t } = useTranslation();

  return (
    <div
      className={cn(
        `relative grid h-6 w-6 shrink-0 place-items-center uppercase ${!logo && "rounded-md bg-[#026292] text-white"} ${classNames ? classNames : ""}`
      )}
    >
      {logo && logo !== "" ? (
        <img
          src={getFileURL(logo)}
          className="absolute left-0 top-0 h-full w-full rounded-md object-cover"
          alt={t("aria_labels.projects_sidebar.workspace_logo")}
        />
      ) : (
        (name?.[0] ?? "...")
      )}
    </div>
  );
};
