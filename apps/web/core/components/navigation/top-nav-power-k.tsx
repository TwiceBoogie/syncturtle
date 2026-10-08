import { cn } from "@syncturtle/ui";
import { Search } from "lucide-react";

export const TopNavPowerK = () => {
  const isOpen = false;
  return (
    <div className="relative">
      <div
        className={cn("relative z-30 flex w-[364px] items-center transition-all duration-300 ease-in-out", {
          "w-[554px]": isOpen,
        })}
      >
        <div
          className={cn(
            "flex h-[28px] w-full items-center rounded-lg border border-custom-border-200 bg-custom-background-90 p-[8px] transition-colors duration-200",
            {
              "bg-custom-background-80": isOpen,
            }
          )}
        >
          <Search className="mr-[8px] size-[14px] shrink-0 text-custom-text-400" />
          <input
            type="text"
            placeholder="Search commands..."
            className="min-w-0 flex-1 bg-transparent text-[13px] leading-[20px] text-custom-text-100 placeholder:text-custom-text-400 outline-none"
          />
        </div>
      </div>
    </div>
  );
};
