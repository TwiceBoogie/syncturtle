"use client";

import type { FC } from "react";

interface IProfileSettingContentHeaderProps {
  title: string;
  description?: string;
}

export const ProfileSettingContentHeader: FC<IProfileSettingContentHeaderProps> = ({ title, description }) => (
  <div className="flex flex-col gap-1 pb-4 border-4 border-custom-border-100 w-full">
    <div className="text-xl font-medium text-custom-text-100">{title}</div>
    {description && <div className="text-sm font-normal text-custom-text-300">{description}</div>}
  </div>
);
