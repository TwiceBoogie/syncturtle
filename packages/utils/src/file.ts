import { API_BASE_PATH } from "@syncturtle/constants";

export const getFileURL = (path: string): string | undefined => {
  if (!path) return undefined;
  const isValidURL = path.startsWith("http");
  if (isValidURL) return path;
  return `${API_BASE_PATH}${path}`;
};
