package com.cricketlive.service;

import com.cricketlive.dto.Dtos.LeaderboardEntry;
import com.cricketlive.dto.Dtos.PlayerDto;
import com.cricketlive.dto.Dtos.PlayerProfile;
import com.cricketlive.dto.Dtos.TeamDto;
import com.cricketlive.exception.NotFoundException;
import com.cricketlive.model.Player;
import com.cricketlive.model.PlayerMatchStats;
import com.cricketlive.repository.PlayerMatchStatsRepository;
import com.cricketlive.repository.PlayerRepository;
import com.cricketlive.repository.TeamRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PlayerService {

    private final TeamRepository teamRepo;
    private final PlayerRepository playerRepo;
    private final PlayerMatchStatsRepository statsRepo;

    public PlayerService(TeamRepository teamRepo, PlayerRepository playerRepo,
                         PlayerMatchStatsRepository statsRepo) {
        this.teamRepo = teamRepo;
        this.playerRepo = playerRepo;
        this.statsRepo = statsRepo;
    }

    public List<TeamDto> teams() {
        return teamRepo.findAllByOrderByNameAsc().stream()
                .map(t -> new TeamDto(t.getId(), t.getName(), t.getShortName()))
                .toList();
    }

    public List<PlayerDto> players(Long teamId, String query) {
        String q = query == null ? "" : query.trim();
        List<Player> found;
        if (teamId != null && !q.isEmpty()) {
            found = playerRepo.findByTeamIdAndNameContainingIgnoreCaseOrderByNameAsc(teamId, q);
        } else if (teamId != null) {
            found = playerRepo.findByTeamIdOrderByNameAsc(teamId);
        } else if (!q.isEmpty()) {
            found = playerRepo.findByNameContainingIgnoreCaseOrderByNameAsc(q);
        } else {
            found = playerRepo.findAllByOrderByNameAsc();
        }
        return found.stream()
                .map(p -> new PlayerDto(p.getId(), p.getName(), p.getRole(), p.getTeam().getShortName()))
                .toList();
    }

    /** Career totals added up over every match the player has featured in. */
    public PlayerProfile profile(Long playerId) {
        Player player = playerRepo.findById(playerId)
                .orElseThrow(() -> new NotFoundException("Player " + playerId + " not found"));
        List<PlayerMatchStats> rows = statsRepo.findByPlayerId(playerId);

        int runs = rows.stream().mapToInt(PlayerMatchStats::getRuns).sum();
        int balls = rows.stream().mapToInt(PlayerMatchStats::getBallsFaced).sum();
        int highScore = rows.stream().mapToInt(PlayerMatchStats::getRuns).max().orElse(0);
        int fours = rows.stream().mapToInt(PlayerMatchStats::getFours).sum();
        int sixes = rows.stream().mapToInt(PlayerMatchStats::getSixes).sum();
        long dismissals = rows.stream().filter(PlayerMatchStats::isDismissed).count();
        int wickets = rows.stream().mapToInt(PlayerMatchStats::getWickets).sum();
        int ballsBowled = rows.stream().mapToInt(PlayerMatchStats::getBallsBowled).sum();
        int conceded = rows.stream().mapToInt(PlayerMatchStats::getRunsConceded).sum();

        Double average = dismissals == 0 ? null : ScoreboardService.round2((double) runs / dismissals);
        double strikeRate = balls == 0 ? 0 : ScoreboardService.round2(runs * 100.0 / balls);
        double economy = ballsBowled == 0 ? 0 : ScoreboardService.round2(conceded * 6.0 / ballsBowled);

        return new PlayerProfile(player.getId(), player.getName(), player.getRole(),
                player.getTeam().getShortName(), rows.size(), runs, balls, highScore, fours, sixes,
                strikeRate, average, wickets, ScoreboardService.overs(ballsBowled), economy);
    }

    public List<LeaderboardEntry> topScorers(int limit) {
        return statsRepo.topScorers(PageRequest.of(0, clamp(limit))).stream()
                .map(this::toEntry)
                .toList();
    }

    public List<LeaderboardEntry> topWicketTakers(int limit) {
        return statsRepo.topWicketTakers(PageRequest.of(0, clamp(limit))).stream()
                .map(this::toEntry)
                .toList();
    }

    private LeaderboardEntry toEntry(Object[] row) {
        return new LeaderboardEntry((Long) row[0], (String) row[1], (String) row[2], ((Number) row[3]).longValue());
    }

    private int clamp(int limit) {
        return Math.max(1, Math.min(limit, 50));
    }
}
