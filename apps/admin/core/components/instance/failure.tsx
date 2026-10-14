"use client";

import { Button } from "@heroui/react";
import { AuthHeader } from "../common/auth-header";

export const InstanceFailureView = () => {
  const handleRetry = () => {
    window.location.reload();
  };

  return (
    <>
      <AuthHeader />
      <div className="flex flex-col justify-center items-center grow w-full py-6 mt-10">
        <div className="relative flex flex-col gap-6 max-w-100 w-full">
          <div className="relative flex flex-col justify-center items-center space-y-4">
            {/* Image */}
            <h3 className="font-medium text-2xl text-center">Unable to fetch instance details.</h3>
            <p className="font-medium text-base text-center">
              We were unable to fetch the details of the instance. Fret not, it might just be a connectivity issue.
            </p>
          </div>
          <div className="flex justify-center">
            <Button className={"rounded-sm"} size="sm" onPress={handleRetry}>
              Retry
            </Button>
          </div>
        </div>
      </div>
    </>
  );
};
