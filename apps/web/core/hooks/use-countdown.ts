import { useState, useEffect, useCallback } from "react";

export const useCountdown = (initialSeconds = 0) => {
  const [seconds, setSeconds] = useState(() => Math.max(initialSeconds, 0));

  useEffect(() => {
    if (seconds <= 0) return;

    const timeoutId = window.setTimeout(() => {
      setSeconds((current) => Math.max(current - 1, 0));
    }, 1000);

    return () => window.clearTimeout(timeoutId);
  }, [seconds]);

  const start = useCallback(
    (nextSeconds = initialSeconds) => {
      setSeconds(Math.max(nextSeconds, 0));
    },
    [initialSeconds]
  );

  const reset = useCallback(() => {
    setSeconds(0);
  }, []);

  return { seconds, isRunning: seconds > 0, start, reset };
};
