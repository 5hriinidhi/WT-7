package com.cricketlive.service;

import com.cricketlive.dto.Dtos.CreateMatchRequest;
import com.cricketlive.dto.Dtos.MatchCard;
import com.cricketlive.exception.ConflictException;
import com.cricketlive.exception.NotFoundException;
import com.cricketlive.live.MatchUpdatedEvent;
import com.cricketlive.model.CricketMatch;
import com.cricketlive.model.MatchStatus;
import com.cricketlive.model.Team;
import com.cricketlive.repository.MatchRepository;
import com.cricketlive.repository.PlayerRepository;
import com.cricketlive.repository.TeamRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MatchService {

    private static final int SQUAD_SIZE = 11;

    private final MatchRepository matchRepo;
    private final TeamRepository teamRepo;
    private final PlayerRepository playerRepo;
    private final ScoreboardService scoreboards;
    private final ApplicationEventPublisher publisher;

    public MatchService(MatchRepository matchRepo, TeamRepository teamRepo, PlayerRepository playerRepo,
                        ScoreboardService scoreboards, ApplicationEventPublisher publisher) {
        this.matchRepo = matchRepo;
        this.teamRepo = teamRepo;
        this.playerRepo = playerRepo;
        this.scoreboards = scoreboards;
        this.publisher = publisher;
    }

    @Transactional
    public MatchCard create(CreateMatchRequest req) {
        if (req.team1Id().equals(req.team2Id())) {
            throw new IllegalArgumentException("A team cannot play itself");
        }
        Team team1 = findTeam(req.team1Id());
        Team team2 = findTeam(req.team2Id());
        for (Team team : List.of(team1, team2)) {
            if (playerRepo.countByTeamId(team.getId()) < SQUAD_SIZE) {
                throw new ConflictException(team.getName() + " needs " + SQUAD_SIZE + " players");
            }
        }

        CricketMatch match = matchRepo.save(
                new CricketMatch(team1, team2, req.venue().trim(), req.totalOvers(), req.simulated()));
        publisher.publishEvent(new MatchUpdatedEvent(match.getId()));
        return scoreboards.card(match.getId());
    }

    /** Switches the built-in feed on or off for a match. */
    @Transactional
    public MatchCard setSimulation(Long matchId, boolean enabled) {
        CricketMatch match = matchRepo.findById(matchId)
                .orElseThrow(() -> new NotFoundException("Match " + matchId + " not found"));
        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new ConflictException("This match is already completed");
        }
        match.setSimulated(enabled);
        publisher.publishEvent(new MatchUpdatedEvent(matchId));
        return scoreboards.card(matchId);
    }

    private Team findTeam(Long id) {
        return teamRepo.findById(id).orElseThrow(() -> new NotFoundException("Team " + id + " not found"));
    }
}
