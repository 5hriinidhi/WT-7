package com.cricketlive.live;

/** Published whenever a match changes, so connected dashboards can be refreshed. */
public record MatchUpdatedEvent(Long matchId) {
}
