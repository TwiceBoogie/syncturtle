import { Metadata } from "next";

export const metadata: Metadata = {
  title: "Sign up - Syncturtle",
  robots: {
    index: true,
    follow: false,
  },
};

export default function SignupLayout({ children }: { children: React.ReactNode }) {
  return children;
}
