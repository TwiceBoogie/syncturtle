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

export interface IFieldViolation {
  field: string;
  message: string;
}

export interface IApiErrorPayload {
  ok: boolean;
  error: string;
  message: string;
  traceId?: string;
  requestId?: string;
  correlationId?: string;
  path?: string;
  timestamp?: string;
  fields?: IFieldViolation[];
}

export type TPaginationInfo = {
  count: number;
  extraStats: string | null;
  nextCursor: string;
  nextPageResults: boolean;
  prevCursor: string;
  prevPageResults: boolean;
  totalPages: number;
  perPage?: number;
  totalResults: number;
};
