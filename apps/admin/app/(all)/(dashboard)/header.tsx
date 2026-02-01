"use client";

import { Breadcrumbs } from "@heroui/react";
import { Settings } from "lucide-react";
import { usePathname } from "next/navigation";

export const AdminHeader = () => {
  const pathName = usePathname();

  const getHeaderTitle = (pathName: string) => {
    switch (pathName) {
      case "general":
        return "General";
      case "email":
        return "Email";
      case "authentication":
        return "Authentication";
      case "image":
        return "Image";
      case "google":
        return "Google";
      case "github":
        return "GitHub";
      case "gitlab":
        return "GitLab";
      case "workspace":
        return "Workspace";
      case "create":
        return "Create";
      default:
        return pathName.toUpperCase();
    }
  };

  const generateBreadcrumbItems = (pathname: string) => {
    const pathSegments = pathname.split("/").slice(1); // removing the first empty string.
    pathSegments.pop();

    let currentUrl = "";
    const breadcrumbItems = pathSegments.map((segment) => {
      currentUrl += "/" + segment;
      return {
        title: getHeaderTitle(segment),
        href: currentUrl,
      };
    });

    return breadcrumbItems;
  };

  const breadcrumbItems = generateBreadcrumbItems(pathName);

  return (
    <div className="relative z-10 flex h-header w-full shrink-0 items-center justify-between gap-x-2 gap-y-4 border-b border-custom-sidebar-border-200 bg-custom-sidebar-background-100 p-4">
      <div className="flex w-full grow items-center gap-2 overflow-ellipsis whitespace-nowrap">
        <Breadcrumbs>
          <Breadcrumbs.Item href={`/general/`}>
            <Settings className="h-4 w-4" /> &nbsp; Settings
          </Breadcrumbs.Item>
          {breadcrumbItems.map(
            (item) =>
              item.title && (
                <Breadcrumbs.Item key={item.title} href={item.href}>
                  {item.title}
                </Breadcrumbs.Item>
              )
          )}
        </Breadcrumbs>
      </div>
    </div>
  );
};
