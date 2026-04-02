import { useWorkspace } from "@/hooks/store/use-workspace";
import { WEB_BASE_URL } from "@syncturtle/constants";
import { getFileURL } from "@syncturtle/utils";
import { ExternalLink } from "lucide-react";
import { FC } from "react";

interface IWorkspaceListItemProps {
  workspaceId: string;
}

export const WorkspaceListItem: FC<IWorkspaceListItemProps> = (props) => {
  const { workspaceId } = props;
  // store hooks
  const { getWorkspaceById } = useWorkspace();
  // derived values
  const workspace = getWorkspaceById(workspaceId);

  if (!workspace) return null;
  return (
    <a
      key={workspaceId}
      href={`${WEB_BASE_URL}/${encodeURIComponent(workspace.slug)}`}
      target="_blank"
      className="group flex items-center justify-between p-4 gap-2.5 truncate border border-custom-border-200/70 hover:border-custom-border-200 hover:bg-custom-background-90 rounded-md"
    >
      <div className="flex items-start gap-4">
        <span
          className={`relative flex h-8 w-8 shrink-0 items-center justify-center p-2 mt-1 text-xs uppercase ${
            !workspace.logoUrl && "rounded bg-custom-primary-500 text-white"
          }`}
        >
          {workspace.logoUrl && workspace.logoUrl !== "" ? (
            <img
              src={getFileURL(workspace.logoUrl)}
              className="absolute left-0 top-0 h-full w-full rounded object-cover"
              alt="Workspace Logo"
            />
          ) : (
            (workspace.name[0] ?? "...")
          )}
        </span>
        <div className="flex flex-col items-start gap-1">
          <div className="flex flex-wrap w-full items-center gap-2.5">
            <h3 className="text-base font-medium capitalize">{workspace.name}</h3>/
          </div>
          {workspace.owner.email && (
            <div className="flex items-center gap-1 text-xs">
              <h3 className="text-custom-text-200 font-medium">Owned by:</h3>
              <h4 className="text-custom-text-300">{workspace.owner.email}</h4>
            </div>
          )}
          <div className="flex items-center gap-2.5 text-xs">
            {workspace.totalMembers !== null && (
              <>
                •
                <span className="flex items-center gap-1">
                  <h3 className="text-custom-text-200 font-medium">Total members:</h3>
                  <h4 className="text-custom-text-300">{workspace.totalMembers}</h4>
                </span>
              </>
            )}
          </div>
        </div>
      </div>
      <div className="shrink-0">
        <ExternalLink size={14} className="text-custom-text-400 group-hover:text-custom-text-200" />
      </div>
    </a>
  );
};
