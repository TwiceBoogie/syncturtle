import { TPaginationInfo } from "../common";
import { IUser } from "../users";

export type TLoader = "init-loader" | "mutation" | "pagination" | "loaded" | undefined;

export interface IWorkspace {
  readonly id: string;
  readonly owner: IUser;
  readonly createdAt: Date;
  readonly updatedAt: Date;
  name: string;
  url: string;
  logoUrl: string | null;
  readonly totalMembers: number;
  readonly slug: string;
  readonly createdById: string;
  readonly updatedById: string;
  organizationSize: string;
  role: number;
}

export type TWorkspacePaginationInfo = TPaginationInfo & {
  results: IWorkspace[];
};

export type TSlugCheckResult = {
  available: boolean;
  message: string;
};

export type TSlugStatus = "idle" | "checking" | "available" | "unavailable";
