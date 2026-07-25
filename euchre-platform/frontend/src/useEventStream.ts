import { useEffect, useRef } from "react";
import { eventStreamUrl } from "./api";

type Handlers = Record<string, (data: unknown) => void>;

/**
 * Subscribe to a tournament's SSE stream. Handlers are keyed by event name
 * (e.g. "score-updated", "leaderboard-updated"). The connection is torn down and
 * re-established whenever the tournament id changes.
 *
 * Handlers are kept in a ref so the EventSource is not recreated on every render.
 */
export function useEventStream(tournamentId: number | null, handlers: Handlers) {
  const handlersRef = useRef(handlers);
  handlersRef.current = handlers;

  useEffect(() => {
    if (tournamentId == null) return;

    const source = new EventSource(eventStreamUrl(tournamentId));

    const listeners: Array<[string, EventListener]> = [];
    Object.keys(handlersRef.current).forEach((eventName) => {
      const listener: EventListener = (event) => {
        const messageEvent = event as MessageEvent;
        let payload: unknown = messageEvent.data;
        try {
          payload = JSON.parse(messageEvent.data);
        } catch {
          /* plain string payload */
        }
        handlersRef.current[eventName]?.(payload);
      };
      source.addEventListener(eventName, listener);
      listeners.push([eventName, listener]);
    });

    return () => {
      listeners.forEach(([name, listener]) => source.removeEventListener(name, listener));
      source.close();
    };
  }, [tournamentId]);
}
