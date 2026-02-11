import * as React from "react";

export function useOutsideClickDetector<T extends HTMLElement>(
  ref: React.RefObject<T | null>,
  callback: (event: MouseEvent) => void,
  useCapture = false
): void {
  const callbackRef = React.useRef(callback);

  React.useEffect(() => {
    callbackRef.current = callback;
  }, [callback]);

  React.useEffect(() => {
    if (typeof document === "undefined") return;

    const handleClick = (event: MouseEvent) => {
      const element = ref.current;
      if (!element) return;

      const target = event.target;
      if (!(target instanceof Node)) return;

      // click inside we ignore
      if (element.contains(target)) return;

      const preventOutsideClickElement =
        target instanceof Element ? target.closest("[data-prevent-outside-click]") : null;

      if (preventOutsideClickElement) return;

      callbackRef.current(event);
    };

    document.addEventListener("pointerdown", handleClick, useCapture);
    return () => {
      document.removeEventListener("pointerdown", handleClick, useCapture);
    };
  }, [ref, useCapture]);
}
