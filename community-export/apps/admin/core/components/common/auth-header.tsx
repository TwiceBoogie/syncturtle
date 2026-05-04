import Link from "next/link";

export const AuthHeader = () => (
  <div className="sticky top-0 flex w-full shrink-0 items-center justify-between gap-6">
    <Link href={`/`}>Syncturtle</Link>
  </div>
);
