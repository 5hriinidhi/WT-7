package com.cricketlive.dto;

import com.cricketlive.model.ExtraType;
import com.cricketlive.model.MatchStatus;
import com.cricketlive.model.PlayerRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/** Request and response shapes of the REST API. */
public final class Dtos {

    private Dtos() {
    }

    // ---------- requests ----------

    /**
     * One delivery. "runs" is what was run or hit off the delivery; the automatic
     * penalty run for a wide or no-ball is added by the server.
     */
    public record BallRequest(
            @Min(0) @Max(6) int runs,
            ExtraType extraType,
            boolean wicket) {
    }

    public record CreateMatchRequest(
            @NotNull Long team1Id,
            @NotNull Long team2Id,
            @NotBlank String venue,
            @Min(1) @Max(50) int totalOvers,
            boolean simulated) {
    }

    // ---------- reference data ----------

    public record TeamDto(Long id, String name, String shortName) {
    }

    public record PlayerDto(Long id, String name, PlayerRole role, String team) {
    }

    public record PlayerProfile(
            Long id, String name, PlayerRole role, String team,
            int matches, int runs, int ballsFaced, int highScore, int fours, int sixes,
            double strikeRate, Double average,
            int wickets, String overs, double economy) {
    }

    public record LeaderboardEntry(Long playerId, String name, String team, long value) {
    }

    // ---------- match list ----------

    public record InningsScore(String team, int runs, int wickets, String overs, boolean current) {
    }

    public record MatchCard(
            Long id, String title, String venue, MatchStatus status, int totalOvers,
            boolean simulated, String team1, String team2,
            List<InningsScore> innings, String result) {
    }

    // ---------- live scoreboard ----------

    public record BatterLine(
            Long playerId, String name, int runs, int balls, int fours, int sixes,
            double strikeRate, String status, boolean onStrike) {
    }

    public record BowlerLine(
            Long playerId, String name, String overs, int runs, int wickets,
            double economy, boolean bowling) {
    }

    public record BallDto(
            int innings, String over, String label, int runs, boolean wicket,
            String extra, String commentary) {
    }

    public record InningsCard(
            int number, String battingTeam, String bowlingTeam,
            int runs, int wickets, String overs, int extras, double runRate,
            Integer target, Integer runsNeeded, Integer ballsLeft, Double requiredRate,
            boolean completed, List<BatterLine> batting, List<BowlerLine> bowling) {
    }

    public record Scoreboard(
            Long matchId, String title, String venue, MatchStatus status, int totalOvers,
            int currentInnings, boolean simulated, String result,
            String team1, String team2,
            List<InningsCard> innings, List<BallDto> recentBalls) {
    }

    // ---------- match summary ----------

    public record Performer(String name, String team, String figures) {
    }

    public record MatchSummary(
            Long matchId, String title, String venue, MatchStatus status, String result,
            List<InningsScore> innings, List<Performer> topBatters, List<Performer> topBowlers) {
    }
}
