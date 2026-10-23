package com.cricketlive.controller;

import com.cricketlive.dto.Dtos.LeaderboardEntry;
import com.cricketlive.dto.Dtos.PlayerDto;
import com.cricketlive.dto.Dtos.PlayerProfile;
import com.cricketlive.dto.Dtos.TeamDto;
import com.cricketlive.service.PlayerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping("/teams")
    public List<TeamDto> teams() {
        return playerService.teams();
    }

    @GetMapping("/players")
    public List<PlayerDto> players(@RequestParam(required = false) Long teamId,
                                   @RequestParam(required = false) String q) {
        return playerService.players(teamId, q);
    }

    @GetMapping("/players/{id}")
    public PlayerProfile player(@PathVariable Long id) {
        return playerService.profile(id);
    }

    @GetMapping("/stats/top-scorers")
    public List<LeaderboardEntry> topScorers(@RequestParam(defaultValue = "5") int limit) {
        return playerService.topScorers(limit);
    }

    @GetMapping("/stats/top-wicket-takers")
    public List<LeaderboardEntry> topWicketTakers(@RequestParam(defaultValue = "5") int limit) {
        return playerService.topWicketTakers(limit);
    }
}
