package com.cricketlive.feed;

import com.cricketlive.model.CricketMatch;
import com.cricketlive.model.MatchStatus;
import com.cricketlive.repository.MatchRepository;
import com.cricketlive.service.ScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Fetches the next delivery from the feed for every live match that has auto-play on. */
@Component
@ConditionalOnProperty(name = "cricket.simulator.enabled", havingValue = "true", matchIfMissing = true)
public class MatchSimulator {

    private static final Logger log = LoggerFactory.getLogger(MatchSimulator.class);

    private final MatchRepository matchRepo;
    private final ScoringService scoringService;
    private final ScoreFeed feed;

    public MatchSimulator(MatchRepository matchRepo, ScoringService scoringService, ScoreFeed feed) {
        this.matchRepo = matchRepo;
        this.scoringService = scoringService;
        this.feed = feed;
    }

    @Scheduled(fixedDelayString = "${cricket.simulator.interval-ms:1500}")
    public void tick() {
        for (CricketMatch match : matchRepo.findByStatusAndSimulatedTrue(MatchStatus.LIVE)) {
            try {
                scoringService.recordBall(match.getId(), feed.nextDelivery());
            } catch (Exception ex) {
                log.warn("Simulator skipped match {}: {}", match.getId(), ex.getMessage());
            }
        }
    }
}
