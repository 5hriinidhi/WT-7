package com.cricketlive.service;

import com.cricketlive.dto.Dtos.BallRequest;
import com.cricketlive.exception.ConflictException;
import com.cricketlive.exception.NotFoundException;
import com.cricketlive.live.MatchUpdatedEvent;
import com.cricketlive.model.BallEvent;
import com.cricketlive.model.CricketMatch;
import com.cricketlive.model.ExtraType;
import com.cricketlive.model.Innings;
import com.cricketlive.model.MatchStatus;
import com.cricketlive.model.Player;
import com.cricketlive.model.PlayerMatchStats;
import com.cricketlive.model.PlayerRole;
import com.cricketlive.model.Team;
import com.cricketlive.repository.BallEventRepository;
import com.cricketlive.repository.InningsRepository;
import com.cricketlive.repository.MatchRepository;
import com.cricketlive.repository.PlayerMatchStatsRepository;
import com.cricketlive.repository.PlayerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The scoring engine. Every delivery goes through recordBall(), which updates the
 * innings, both players' figures, the ball log and (when needed) the match result
 * inside a single transaction while holding a lock on the match row.
 */
@Service
public class ScoringService {

    private static final int BALLS_PER_OVER = 6;
    private static final int MAX_WICKETS = 10;

    private final MatchRepository matchRepo;
    private final InningsRepository inningsRepo;
    private final BallEventRepository ballRepo;
    private final PlayerRepository playerRepo;
    private final PlayerMatchStatsRepository statsRepo;
    private final ApplicationEventPublisher publisher;

    public ScoringService(MatchRepository matchRepo, InningsRepository inningsRepo,
                          BallEventRepository ballRepo, PlayerRepository playerRepo,
                          PlayerMatchStatsRepository statsRepo, ApplicationEventPublisher publisher) {
        this.matchRepo = matchRepo;
        this.inningsRepo = inningsRepo;
        this.ballRepo = ballRepo;
        this.playerRepo = playerRepo;
        this.statsRepo = statsRepo;
        this.publisher = publisher;
    }

    @Transactional
    public void startMatch(Long matchId) {
        CricketMatch match = lockMatch(matchId);
        if (match.getStatus() != MatchStatus.UPCOMING) {
            throw new ConflictException("This match has already started");
        }
        match.setStatus(MatchStatus.LIVE);
        match.setCurrentInnings(1);
        openInnings(match, 1, match.getTeam1(), match.getTeam2(), null);
        publisher.publishEvent(new MatchUpdatedEvent(matchId));
    }

    /** Records one delivery and returns the status the match is in afterwards. */
    @Transactional
    public MatchStatus recordBall(Long matchId, BallRequest req) {
        CricketMatch match = lockMatch(matchId);
        if (match.getStatus() != MatchStatus.LIVE) {
            throw new ConflictException("Match is not live");
        }

        ExtraType extra = req.extraType() == null ? ExtraType.NONE : req.extraType();
        validate(req, extra);

        Innings innings = inningsRepo.findByMatchIdAndInningsNumber(matchId, match.getCurrentInnings())
                .orElseThrow(() -> new IllegalStateException("No innings found for match " + matchId));

        Player batter = innings.getStriker();
        Player bowler = innings.getBowler();
        int runs = req.runs();
        boolean wicket = req.wicket();
        boolean legal = extra != ExtraType.WIDE && extra != ExtraType.NO_BALL;
        int total = runs + (legal ? 0 : 1);

        int overNumber = innings.getLegalBalls() / BALLS_PER_OVER;
        int ballsInOver = innings.getLegalBalls() % BALLS_PER_OVER;

        // these two are managed entities, so changes are flushed when the transaction commits
        PlayerMatchStats bat = statsFor(match, batter);
        PlayerMatchStats bowl = statsFor(match, bowler);

        if (extra != ExtraType.WIDE) {
            bat.setBallsFaced(bat.getBallsFaced() + 1);
        }
        if (extra == ExtraType.NONE || extra == ExtraType.NO_BALL) {
            bat.setRuns(bat.getRuns() + runs);
            if (runs == 4) bat.setFours(bat.getFours() + 1);
            if (runs == 6) bat.setSixes(bat.getSixes() + 1);
        }
        // byes and leg byes are not charged to the bowler
        if (extra == ExtraType.NONE || extra == ExtraType.WIDE || extra == ExtraType.NO_BALL) {
            bowl.setRunsConceded(bowl.getRunsConceded() + total);
        }
        if (legal) {
            bowl.setBallsBowled(bowl.getBallsBowled() + 1);
            innings.setLegalBalls(innings.getLegalBalls() + 1);
        }
        if (wicket) {
            bat.setDismissed(true);
            bowl.setWickets(bowl.getWickets() + 1);
            innings.setWickets(innings.getWickets() + 1);
        }
        innings.setRuns(innings.getRuns() + total);
        innings.setExtras(innings.getExtras() + extraRuns(extra, runs));

        int ballNumber = legal ? ballsInOver + 1 : ballsInOver;
        ballRepo.save(new BallEvent(match, innings.getInningsNumber(), overNumber, ballNumber,
                batter, bowler, total, extra, wicket, describe(bowler, batter, extra, runs, wicket)));

        boolean inningsOver = isInningsOver(match, innings);
        if (!inningsOver) {
            if (wicket) {
                bringInNextBatter(match, innings);
            }
            if (runs % 2 == 1) {
                swapEnds(innings);
            }
            if (legal && innings.getLegalBalls() % BALLS_PER_OVER == 0) {
                swapEnds(innings);
                innings.setBowler(pickBowler(innings.getBowlingTeam(), innings.getLegalBalls() / BALLS_PER_OVER));
            }
        }
        inningsRepo.save(innings);

        if (inningsOver) {
            finishInnings(match, innings);
        }

        publisher.publishEvent(new MatchUpdatedEvent(matchId));
        return match.getStatus();
    }

    // ------------------------------------------------------------------

    private CricketMatch lockMatch(Long matchId) {
        return matchRepo.findByIdForUpdate(matchId)
                .orElseThrow(() -> new NotFoundException("Match " + matchId + " not found"));
    }

    private void validate(BallRequest req, ExtraType extra) {
        if (req.wicket() && (extra != ExtraType.NONE || req.runs() != 0)) {
            throw new IllegalArgumentException("A wicket can only fall on a legal delivery with no runs");
        }
        if ((extra == ExtraType.BYE || extra == ExtraType.LEG_BYE) && req.runs() < 1) {
            throw new IllegalArgumentException("Byes and leg byes need at least 1 run");
        }
    }

    // runs that go into the extras column of the scorecard
    private int extraRuns(ExtraType extra, int runs) {
        return switch (extra) {
            case WIDE -> runs + 1;
            case NO_BALL -> 1;
            case BYE, LEG_BYE -> runs;
            case NONE -> 0;
        };
    }

    private boolean isInningsOver(CricketMatch match, Innings innings) {
        boolean allOut = innings.getWickets() >= MAX_WICKETS;
        boolean oversUp = innings.getLegalBalls() >= match.getTotalOvers() * BALLS_PER_OVER;
        boolean targetChased = innings.getTarget() != null && innings.getRuns() >= innings.getTarget();
        return allOut || oversUp || targetChased;
    }

    private void finishInnings(CricketMatch match, Innings innings) {
        innings.setCompleted(true);
        if (innings.getInningsNumber() == 1) {
            match.setCurrentInnings(2);
            openInnings(match, 2, innings.getBowlingTeam(), innings.getBattingTeam(), innings.getRuns() + 1);
        } else {
            match.setStatus(MatchStatus.COMPLETED);
            match.setResult(buildResult(innings));
        }
        matchRepo.save(match);
    }

    private String buildResult(Innings secondInnings) {
        int target = secondInnings.getTarget();
        if (secondInnings.getRuns() >= target) {
            int wicketsLeft = MAX_WICKETS - secondInnings.getWickets();
            return secondInnings.getBattingTeam().getName() + " won by " + wicketsLeft
                    + (wicketsLeft == 1 ? " wicket" : " wickets");
        }
        int margin = target - 1 - secondInnings.getRuns();
        if (margin == 0) {
            return "Match tied";
        }
        return secondInnings.getBowlingTeam().getName() + " won by " + margin + (margin == 1 ? " run" : " runs");
    }

    private Innings openInnings(CricketMatch match, int number, Team batting, Team bowling, Integer target) {
        List<Player> order = playerRepo.findByTeamIdOrderByIdAsc(batting.getId());
        if (order.size() < 2) {
            throw new ConflictException(batting.getName() + " needs at least 2 players");
        }
        Innings innings = new Innings(match, number, batting, bowling);
        innings.setStriker(order.get(0));
        innings.setNonStriker(order.get(1));
        innings.setNextBatterIndex(2);
        innings.setBowler(pickBowler(bowling, 0));
        innings.setTarget(target);
        innings = inningsRepo.save(innings);

        // create the openers' rows now so they appear on the scorecard at 0 (0)
        statsFor(match, order.get(0));
        statsFor(match, order.get(1));
        return innings;
    }

    private void bringInNextBatter(CricketMatch match, Innings innings) {
        List<Player> order = playerRepo.findByTeamIdOrderByIdAsc(innings.getBattingTeam().getId());
        int index = innings.getNextBatterIndex();
        Player next = order.get(index);
        innings.setStriker(next);
        innings.setNextBatterIndex(index + 1);
        statsFor(match, next);
    }

    // bowlers (and all-rounders) take the overs in turn, so nobody bowls two in a row
    private Player pickBowler(Team bowlingTeam, int overIndex) {
        List<Player> squad = playerRepo.findByTeamIdOrderByIdAsc(bowlingTeam.getId());
        List<Player> options = squad.stream()
                .filter(p -> p.getRole() == PlayerRole.BOWLER || p.getRole() == PlayerRole.ALL_ROUNDER)
                .toList();
        if (options.isEmpty()) {
            options = squad;
        }
        return options.get(overIndex % options.size());
    }

    private void swapEnds(Innings innings) {
        Player previousStriker = innings.getStriker();
        innings.setStriker(innings.getNonStriker());
        innings.setNonStriker(previousStriker);
    }

    private PlayerMatchStats statsFor(CricketMatch match, Player player) {
        return statsRepo.findByMatchIdAndPlayerId(match.getId(), player.getId())
                .orElseGet(() -> statsRepo.save(new PlayerMatchStats(match, player)));
    }

    private String describe(Player bowler, Player batter, ExtraType extra, int runs, boolean wicket) {
        String line = bowler.getName() + " to " + batter.getName() + ", ";
        if (wicket) {
            return line + "OUT! " + batter.getName() + " has to go";
        }
        switch (extra) {
            case WIDE:
                return line + "wide" + (runs > 0 ? " and " + runs + " more" : "");
            case NO_BALL:
                return line + "no ball" + (runs > 0 ? ", " + runs + " off the bat" : "");
            case BYE:
                return line + runs + (runs == 1 ? " bye" : " byes");
            case LEG_BYE:
                return line + runs + (runs == 1 ? " leg bye" : " leg byes");
            default:
                if (runs == 0) return line + "no run";
                if (runs == 4) return line + "FOUR!";
                if (runs == 6) return line + "SIX!";
                return line + runs + (runs == 1 ? " run" : " runs");
        }
    }
}
