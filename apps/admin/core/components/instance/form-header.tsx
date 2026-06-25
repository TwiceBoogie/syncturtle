import type { FC } from "react";

interface IFormHeaderProps {
  heading: string;
  subHeading: string;
}
export const FormHeader: FC<IFormHeaderProps> = (props) => {
  const { heading, subHeading } = props;
  return (
    <div className="">
      <span>{heading}</span>
      <span>{subHeading}</span>
    </div>
  );
};
