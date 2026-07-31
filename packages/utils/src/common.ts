import type { Listener, Unsubscribe } from "@syncturtle/types";

/**
 * pub/sub emitter used by external stores.
 *
 * - subscribe(listener) -> returns unsubscribe()
 * - emit() -> calls all listeners
 *
 * In a react external store, `emit()` should be called AFTER updating snapshot.
 */
export class Emitter {
  private listeners = new Set<Listener>();

  /**
   * Registers a listener to be called on every store update.
   * Returns a `Unsubscribe` function that removes that listener.
   */
  public subscribe = (listener: Listener): Unsubscribe => {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  };

  /**
   * Notify all listeners that something changed.
   *
   * React will call `getSnapshot` again and compare values.
   */
  public emit = (): void => this.listeners.forEach((listener) => listener());
}
