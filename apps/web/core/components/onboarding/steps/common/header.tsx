import type { FC } from "react";

interface ICommonOnboardingHeaderProps {
  title: string;
  description: string;
}

export const CommonOnboardingHeader: FC<ICommonOnboardingHeaderProps> = ({ title, description }) => (
  <div className="text-left space-y-2">
    <h1 className="text-2xl font-semibold text-custom-text-200">{title}</h1>
    <p className="text-base text-custom-text-300">{description}</p>
  </div>
);
