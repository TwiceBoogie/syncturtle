import type { ReactNode } from "react";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Onboarding",
};

export default function OnboardingLayout({ children }: { children: ReactNode }) {
  return <>{children}</>;
}
