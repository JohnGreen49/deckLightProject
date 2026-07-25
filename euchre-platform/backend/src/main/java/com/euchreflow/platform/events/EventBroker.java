package com.euchreflow.platform.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory publish/subscribe hub backed by Server-Sent Events, keyed by tournament id.
 *
 * <p>Player pages and the leaderboard open one {@link SseEmitter} each via {@link #subscribe};
 * services call {@link #publish} when something changes and every open connection for that
 * tournament receives the event. Dead emitters are pruned lazily on the next publish.
 *
 * <p>This is deliberately single-node. To scale horizontally, back {@code publish} with a shared
 * bus (Postgres {@code LISTEN/NOTIFY}, Redis pub/sub, etc.) and keep emitters node-local.
 */
@Component
public class EventBroker {

    private static final Logger log = LoggerFactory.getLogger(EventBroker.class);
    private static final long TIMEOUT_MS = 30 * 60 * 1000L; // 30 minutes

    private final Map<Long, List<SseEmitter>> emittersByTournament = new ConcurrentHashMap<>();

    /** Open a live stream for a tournament. The caller returns this from an SSE endpoint. */
    public SseEmitter subscribe(Long tournamentId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        List<SseEmitter> list = emittersByTournament.computeIfAbsent(
                tournamentId, k -> new CopyOnWriteArrayList<>());
        list.add(emitter);

        emitter.onCompletion(() -> list.remove(emitter));
        emitter.onTimeout(() -> {
            list.remove(emitter);
            emitter.complete();
        });
        emitter.onError(e -> list.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            list.remove(emitter);
        }
        return emitter;
    }

    /** Broadcast an event to every open connection for the tournament. */
    public void publish(Long tournamentId, TournamentEvent event) {
        List<SseEmitter> list = emittersByTournament.get(tournamentId);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name(event.type()).data(event.payload()));
            } catch (IOException | IllegalStateException e) {
                list.remove(emitter);
                log.debug("Dropped dead SSE emitter for tournament {}", tournamentId);
            }
        }
    }
}
