import type { FC, ReactNode } from "react";
// import { useMember } from "@/hooks/store/use-member";
// import { useWorkspace } from "@/hooks/store/use-workspace";
// import { useUser } from "@/hooks/store/user";
// import { usePlatformOs } from "@/hooks/use-platform-os";
// import { useTheme } from "next-themes";
// import { useParams } from "next/navigation";
// import { Button, Spinner } from "@heroui/react";
// import { LogOut } from "lucide-react";
// import Link from "next/link";

interface IWorkspaceAuthWrapper {
  children: ReactNode;
  isLoading?: boolean;
}

export const WorkspaceAuthWrapper: FC<IWorkspaceAuthWrapper> = (props) => {
  const { children, isLoading: isParentLoading = false } = props;
  console.log(isParentLoading);
  // const { workspaceSlug } = useParams();
  // const { resolvedTheme } = useTheme();
  // // store hooks
  // const { signOut, data: currentUser } = useUser();
  // const {
  //   workspace: { fetchWorkspaceMembers },
  // } = useMember();
  // const { workspaces } = useWorkspace();
  // const { isMobile } = usePlatformOs();
  // if list of workspaces are not there then we have to render the spinner
  // return (
  //   <div className="grid h-full place-items-center bg-custom-background-100 p-4 rounded-lg border border-custom-border-200">
  //     <div className="flex flex-col items-center gap-3 text-center">
  //       <Spinner />
  //     </div>
  //   </div>
  // );

  // if workspaces are there and we are trying to access the workspace that we are not part of then show the existing workspaces
  // return (
  //   <div className="relative flex h-full w-full flex-col items-center justify-center bg-custom-background-90">
  //     <div className="container relative mx-auto flex h-full w-full flex-col overflow-hidden overflow-y-auto px-5 py-14 md:px-0">
  //       <div className="relative flex shrink-0 items-center justify-between gap-4">
  //         <div className="z-10 shrink-0 bg-custom-background-90 py-4">
  //         </div>
  //         <div className="relative flex items-center gap-2">
  //           <div className="text-sm font-medium">lunasnow@marvel.com</div>
  //           <div className="relative flex h-6 w-6 shrink-0 cursor-pointer items-center justify-center overflow-hidden rounded hover:bg-custom-background-80">
  //             <LogOut size={14} />
  //           </div>
  //         </div>
  //       </div>
  //       <div className="relative flex h-full w-full grow flex-col items-center justify-center space-y-3">
  //         <div className="relative shrink-0">{/*image */}</div>
  //         <h3 className="text-center text-lg font-semibold">Workspace not found</h3>
  //         <p className="text-center text-sm text-custom-text-200">
  //           No workspace found with the URL. It may not exist or you lack authorization to view it.
  //         </p>
  //         <div className="flex items-center justify-center gap-2 pt-4">
  //           <Link href={`/`}>
  //             <Button size="sm" type="button" className={"rounded-sm"}>
  //               Go Home
  //             </Button>
  //           </Link>
  //           <Link href={`/`}>
  //             <Button size="sm" type="button" className={"rounded-sm"}>
  //               Visit Profile
  //             </Button>
  //           </Link>
  //           <Link href={`/`}>
  //             <Button size="sm" type="button" className={"rounded-sm"}>
  //               Create new workspace
  //             </Button>
  //           </Link>
  //         </div>
  //       </div>
  //       <div className="absolute bottom-0 left-4 top-0 w-0 bg-custom-background-80 md:w-0.5" />
  //     </div>
  //   </div>
  // );

  // while user does not have access to view that workspace
  // return (
  //   <div className="h-screen w-full overflow-hidden bg-custom-background-100">
  //     <div className="grid h-full place-items-center p-4">
  //       <div className="space-y-8 text-center">
  //         <div className="space-y-2">
  //           <h3 className="text-lg font-semibold">Not Authorized!</h3>
  //           <p className="mx-auto w-1/2 text-sm text-custom-text-200">
  //             You{"'"}re not a member of this workspace. Please contact the workspace admin to get an invitation or
  //             check your pending invitations.
  //           </p>
  //         </div>
  //         <div className="flex items-center justify-center gap-2">
  //           <Link href={`/invitations`}>
  //             <Button type="button" size="sm" className={`rounded-sm`}>
  //               Check pending invites
  //             </Button>
  //           </Link>
  //           <Link href={`/create-workspace`}>
  //             <Button type="button" size="sm" className={`rounded-sm`}>
  //               Create new workspace
  //             </Button>
  //           </Link>
  //         </div>
  //       </div>
  //     </div>
  //   </div>
  // );

  return <>{children}</>;
};
