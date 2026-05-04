import { useEffect } from "react";

interface IUseHeadProps {
  title?: string;
}

export const useHead = ({ title }: IUseHeadProps) => {
  useEffect(() => {
    if (title) {
      document.title = title ?? "SyncTurtle | The open source Life OS";
    }
  }, [title]);
};
