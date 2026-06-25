"use client";

import type { FC, ReactNode } from "react";
// heroui
import { Button, toast } from "@heroui/react";
import { Copy } from "lucide-react";

interface ICopyFieldProps {
  label: string;
  url: string;
  description: string | ReactNode;
}

export interface ICopyField {
  key: string;
  label: string;
  url: string;
  description: string | ReactNode;
}

export const CopyField: FC<ICopyFieldProps> = (props) => {
  const { label, url, description } = props;

  return (
    <div className="flex flex-col gap-1">
      <h4 className="text-sm text-custom-text-200">{label}</h4>
      <Button
        variant="outline"
        fullWidth
        className={`flex items-center justify-between`}
        onPress={() => {
          navigator.clipboard.writeText(url);
          toast("Copied to clipboard", {
            actionProps: {
              children: "Dismiss",
              onPress: () => toast.clear(),
            },
            description: `The ${label} has been successfully copied to your clipboard`,
            variant: "success",
          });
        }}
      >
        <p className="text-sm font-medium">{url}</p>
        <Copy size={18} color="#B9B9B9" />
      </Button>
      <div className="text-xs text-custom-text-300">{description}</div>
    </div>
  );
};
