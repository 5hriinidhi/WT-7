package com.cricketlive.controller;

import com.cricketlive.dto.Dtos.BallDto;
import com.cricketlive.dto.Dtos.BallRequest;
import com.cricketlive.dto.Dtos.CreateMatchRequest;
import com.cricketlive.dto.Dtos.MatchCard;
import com.cricketlive.dto.Dtos.MatchSummary;
import com.cricketlive.dto.Dtos.Scoreboard;
import com.cricketlive.model.MatchStatus;
import com.cricketlive.service.MatchService;
import com.cricketlive.service.ScoreboardService;
import com.cricketlive.service.ScoringService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final ScoringService scoringService;
    private final ScoreboardService scoreboards;

    public MatchController(MatchService matchService, ScoringService scoringService, ScoreboardService scoreboards) {
        this.matchService = matchService;
        this.scoringService = scoringService;
        this.scoreboards = scoreboards;
    }

    @GetMapping
    public List<MatchCard> list(@RequestParam(required = false) MatchStatus status,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return scoreboards.cards(status, Math.max(0, page), Math.min(Math.max(1, size), 100));
    }

    @GetMapping("/{id}")
    public Scoreboard get(@PathVariable Long id) {
        return scoreboards.scoreboard(id);
    }

    @GetMapping("/{id}/summary")
    public MatchSummary summary(@PathVariable Long id) {
        return scoreboards.summary(id);
    }

    @GetMapping("/{id}/balls")
    public List<BallDto> balls(@PathVariable Long id, @RequestParam(defaultValue = "30") int limit) {
        return scoreboards.commentary(id, limit);
    }

    @PostMapping
    public ResponseEntity<MatchCard> create(@Valid @RequestBody CreateMatchRequest request) {
        MatchCard card = matchService.create(request);
        return ResponseEntity.created(URI.create("/api/matches/" + card.id())).body(card);
    }

    @PostMapping("/{id}/start")
    public MatchCard start(@PathVariable Long id) {
        scoringService.startMatch(id);
        return scoreboards.card(id);
    }

    @PostMapping("/{id}/balls")
    public Scoreboard addBall(@PathVariable Long id, @Valid @RequestBody BallRequest request) {
        scoringService.recordBall(id, request);
        return scoreboards.scoreboard(id);
    }

    @PutMapping("/{id}/simulation")
    public MatchCard simulation(@PathVariable Long id, @RequestParam boolean enabled) {
        return matchService.setSimulation(id, enabled);
    }
}
