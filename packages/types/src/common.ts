/**
 * Unsubscribe function returbed by `subscribe`.
 *
 * Calling it removes the listener from the store so it no longer receives updates.
 */
export type Unsubscribe = () => void;

/**
 * Listener called whenever the external store changes.
 *
 * React will provide a callback to the `subscribe` function. That callback
 * triggers React's internal "check snapshot + rerender if chaged".
 */
export type Listener = () => void;
