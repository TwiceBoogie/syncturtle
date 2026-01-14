"use client";

import { useInstance } from "@/hooks/store/use-instance";

export default function Home() {
  // store hooks
  const { instance, error } = useInstance();

  if (!instance && !error) {
    return <div className="flex">Spinner</div>;
  }

  return <div>hello</div>;
}
