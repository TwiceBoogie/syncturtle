import { useEffect } from "react";

const DEFAULT_TITLE = "Syncturtle | The open source Life OS";

interface IUseHeadProps {
  title?: string | undefined;
}

export const useHead = ({ title }: IUseHeadProps) => {
  useEffect(() => {
    if (title) {
      document.title = title?.trim() ? title : DEFAULT_TITLE;
    }
  }, [title]);
};
