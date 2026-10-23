package com.cricketlive.live;

import com.cricketlive.dto.Dtos.MatchCard;
import com.cricketlive.dto.Dtos.Scoreboard;
import com.cricketlive.service.ScoreboardService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pushes score changes to every open dashboard over Server-Sent Events.
 * Each browser either watches the match list only (matchId = null) or a single match.
 */
@Component
public class LiveUpdateBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(LiveUpdateBroadcaster.class);

    private record Subscriber(SseEmitter emitter, Long matchId) {
    }

    private final Set<Subscriber> subscribers = new CopyOnWriteArraySet<>();

    // a single sender thread keeps updates in order and stops one slow browser from blocking scoring
    private final ExecutorService sender = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "sse-sender");
        thread.setDaemon(true);
        return thread;
    });

    private final ScoreboardService scoreboards;

    public LiveUpdateBroadcaster(ScoreboardService scoreboards) {
        this.scoreboards = scoreboards;
    }

    public SseEmitter subscribe(Long matchId) {
        SseEmitter emitter = new SseEmitter(0L); // never times out; dead connections are dropped on the next send
        Subscriber subscriber = new Subscriber(emitter, matchId);
        subscribers.add(subscriber);

        emitter.onCompletion(() -> subscribers.remove(subscriber));
        emitter.onTimeout(() -> subscribers.remove(subscriber));
        emitter.onError(ex -> subscribers.remove(subscriber));

        try {
            emitter.send(SseEmitter.event().name("hello").data("connected"));
        } catch (IOException ex) {
            subscribers.remove(subscriber);
        }
        return emitter;
    }

    // runs only after the scoring transaction has committed, so readers never see half-written data
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onMatchUpdated(MatchUpdatedEvent event) {
        sender.submit(() -> push(event.matchId()));
    }

    private void push(Long matchId) {
        if (subscribers.isEmpty()) {
            return;
        }
        try {
            MatchCard card = scoreboards.card(matchId);
            boolean someoneWatching = subscribers.stream().anyMatch(s -> matchId.equals(s.matchId()));
            Scoreboard board = someoneWatching ? scoreboards.scoreboard(matchId) : null;

            for (Subscriber subscriber : subscribers) {
                send(subscriber, "card", card);
                if (board != null && matchId.equals(subscriber.matchId())) {
                    send(subscriber, "score", board);
                }
            }
        } catch (Exception ex) {
            log.warn("Could not broadcast update for match {}: {}", matchId, ex.getMessage());
        }
    }

    private void send(Subscriber subscriber, String eventName, Object payload) {
        try {
            subscriber.emitter().send(SseEmitter.event().name(eventName).data(payload, MediaType.APPLICATION_JSON));
        } catch (Exception ex) {
            subscribers.remove(subscriber);
        }
    }

    // keeps idle connections open through proxies and notices browsers that went away
    @Scheduled(fixedRate = 20000)
    public void heartbeat() {
        for (Subscriber subscriber : subscribers) {
            try {
                subscriber.emitter().send(SseEmitter.event().comment("ping"));
            } catch (Exception ex) {
                subscribers.remove(subscriber);
            }
        }
    }

    @PreDestroy
    void shutdown() {
        sender.shutdownNow();
        subscribers.forEach(s -> s.emitter().complete());
    }
}
