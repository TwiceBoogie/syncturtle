import type { FC } from "react";
import { useHead } from "@syncturtle/ui";

interface IPageHead {
  title?: string | undefined;
}

export const PageHead: FC<IPageHead> = (props) => {
  const { title } = props;

  useHead({ title });

  return null;
};
