package com.cricketlive.controller;

import com.cricketlive.live.LiveUpdateBroadcaster;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final LiveUpdateBroadcaster broadcaster;

    public StreamController(LiveUpdateBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    /**
     * Server-Sent Events. Every client gets "card" events (match list changes);
     * pass matchId to also receive full "score" events for that match.
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) Long matchId) {
        return broadcaster.subscribe(matchId);
    }
}
