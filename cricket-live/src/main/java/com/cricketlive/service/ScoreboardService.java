package com.cricketlive.service;

import com.cricketlive.dto.Dtos.BallDto;
import com.cricketlive.dto.Dtos.BatterLine;
import com.cricketlive.dto.Dtos.BowlerLine;
import com.cricketlive.dto.Dtos.InningsCard;
import com.cricketlive.dto.Dtos.InningsScore;
import com.cricketlive.dto.Dtos.MatchCard;
import com.cricketlive.dto.Dtos.MatchSummary;
import com.cricketlive.dto.Dtos.Performer;
import com.cricketlive.dto.Dtos.Scoreboard;
import com.cricketlive.exception.NotFoundException;
import com.cricketlive.model.BallEvent;
import com.cricketlive.model.CricketMatch;
import com.cricketlive.model.ExtraType;
import com.cricketlive.model.Innings;
import com.cricketlive.model.MatchStatus;
import com.cricketlive.model.Player;
import com.cricketlive.model.PlayerMatchStats;
import com.cricketlive.repository.BallEventRepository;
import com.cricketlive.repository.InningsRepository;
import com.cricketlive.repository.MatchRepository;
import com.cricketlive.repository.PlayerMatchStatsRepository;
import com.cricketlive.repository.PlayerRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Read side of the app: turns the stored rows into the shapes the dashboard needs. */
@Service
@Transactional(readOnly = true)
public class ScoreboardService {

    private final MatchRepository matchRepo;
    private final InningsRepository inningsRepo;
    private final BallEventRepository ballRepo;
    private final PlayerMatchStatsRepository statsRepo;
    private final PlayerRepository playerRepo;

    public ScoreboardService(MatchRepository matchRepo, InningsRepository inningsRepo,
                             BallEventRepository ballRepo, PlayerMatchStatsRepository statsRepo,
                             PlayerRepository playerRepo) {
        this.matchRepo = matchRepo;
        this.inningsRepo = inningsRepo;
        this.ballRepo = ballRepo;
        this.statsRepo = statsRepo;
        this.playerRepo = playerRepo;
    }

    public MatchCard card(Long matchId) {
        CricketMatch match = find(matchId);
        return toCard(match, inningsRepo.findByMatchIdOrderByInningsNumber(matchId));
    }

    public List<MatchCard> cards(MatchStatus status, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        List<CricketMatch> matches = status == null
                ? matchRepo.findAll(pageRequest).getContent()
                : matchRepo.findByStatus(status, pageRequest).getContent();
        if (matches.isEmpty()) {
            return List.of();
        }

        // one query for the innings of every match on the page instead of one per match
        List<Long> ids = matches.stream().map(CricketMatch::getId).toList();
        Map<Long, List<Innings>> inningsByMatch = inningsRepo.findByMatchIdInOrderByInningsNumber(ids).stream()
                .collect(Collectors.groupingBy(i -> i.getMatch().getId()));

        return matches.stream()
                .map(m -> toCard(m, inningsByMatch.getOrDefault(m.getId(), List.of())))
                .toList();
    }

    public Scoreboard scoreboard(Long matchId) {
        CricketMatch match = find(matchId);
        List<Innings> innings = inningsRepo.findByMatchIdOrderByInningsNumber(matchId);
        Map<Long, PlayerMatchStats> stats = statsRepo.findByMatchId(matchId).stream()
                .collect(Collectors.toMap(s -> s.getPlayer().getId(), Function.identity()));

        List<InningsCard> cards = new ArrayList<>();
        for (Innings i : innings) {
            cards.add(toInningsCard(match, i, stats));
        }

        List<BallEvent> latest = new ArrayList<>(ballRepo.findTop12ByMatchIdOrderByIdDesc(matchId));
        Collections.reverse(latest);

        return new Scoreboard(match.getId(), match.getTitle(), match.getVenue(), match.getStatus(),
                match.getTotalOvers(), match.getCurrentInnings(), match.isSimulated(), match.getResult(),
                match.getTeam1().getShortName(), match.getTeam2().getShortName(),
                cards, latest.stream().map(this::toBallDto).toList());
    }

    /** Ball-by-ball commentary, newest first. */
    public List<BallDto> commentary(Long matchId, int limit) {
        find(matchId);
        int size = Math.max(1, Math.min(limit, 200));
        return ballRepo.findByMatchIdOrderByIdDesc(matchId, PageRequest.of(0, size)).stream()
                .map(this::toBallDto)
                .toList();
    }

    // A finished match never changes, so its summary is cached after the first request.
    @Cacheable(cacheNames = "matchSummary", key = "#matchId",
            unless = "#result.status() != T(com.cricketlive.model.MatchStatus).COMPLETED")
    public MatchSummary summary(Long matchId) {
        CricketMatch match = find(matchId);
        List<Innings> innings = inningsRepo.findByMatchIdOrderByInningsNumber(matchId);
        List<PlayerMatchStats> rows = statsRepo.findByMatchId(matchId);

        List<Performer> batters = rows.stream()
                .filter(s -> s.getBallsFaced() > 0)
                .sorted(Comparator.comparingInt(PlayerMatchStats::getRuns).reversed()
                        .thenComparingInt(PlayerMatchStats::getBallsFaced))
                .limit(3)
                .map(s -> new Performer(s.getPlayer().getName(), s.getPlayer().getTeam().getShortName(),
                        s.getRuns() + " (" + s.getBallsFaced() + ")"))
                .toList();

        List<Performer> bowlers = rows.stream()
                .filter(s -> s.getBallsBowled() > 0)
                .sorted(Comparator.comparingInt(PlayerMatchStats::getWickets).reversed()
                        .thenComparingInt(PlayerMatchStats::getRunsConceded))
                .limit(3)
                .map(s -> new Performer(s.getPlayer().getName(), s.getPlayer().getTeam().getShortName(),
                        s.getWickets() + "/" + s.getRunsConceded() + " (" + overs(s.getBallsBowled()) + ")"))
                .toList();

        return new MatchSummary(match.getId(), match.getTitle(), match.getVenue(), match.getStatus(),
                match.getResult(), scores(match, innings), batters, bowlers);
    }

    // ------------------------------------------------------------------

    private CricketMatch find(Long id) {
        return matchRepo.findById(id).orElseThrow(() -> new NotFoundException("Match " + id + " not found"));
    }

    private MatchCard toCard(CricketMatch match, List<Innings> innings) {
        return new MatchCard(match.getId(), match.getTitle(), match.getVenue(), match.getStatus(),
                match.getTotalOvers(), match.isSimulated(),
                match.getTeam1().getShortName(), match.getTeam2().getShortName(),
                scores(match, innings), match.getResult());
    }

    private List<InningsScore> scores(CricketMatch match, List<Innings> innings) {
        return innings.stream()
                .map(i -> new InningsScore(i.getBattingTeam().getShortName(), i.getRuns(), i.getWickets(),
                        overs(i.getLegalBalls()),
                        match.getStatus() == MatchStatus.LIVE && i.getInningsNumber() == match.getCurrentInnings()))
                .toList();
    }

    private InningsCard toInningsCard(CricketMatch match, Innings innings, Map<Long, PlayerMatchStats> stats) {
        // batters who have come in so far, in batting order
        List<Player> battingSquad = playerRepo.findByTeamIdOrderByIdAsc(innings.getBattingTeam().getId());
        List<BatterLine> batting = new ArrayList<>();
        int cameIn = Math.min(innings.getNextBatterIndex(), battingSquad.size());
        for (int idx = 0; idx < cameIn; idx++) {
            Player p = battingSquad.get(idx);
            PlayerMatchStats s = stats.get(p.getId());
            int runs = s == null ? 0 : s.getRuns();
            int balls = s == null ? 0 : s.getBallsFaced();
            boolean out = s != null && s.isDismissed();
            String status = out ? "out" : (innings.isCompleted() ? "not out" : "batting");
            boolean onStrike = !innings.isCompleted() && p.getId().equals(innings.getStriker().getId());
            batting.add(new BatterLine(p.getId(), p.getName(), runs, balls,
                    s == null ? 0 : s.getFours(), s == null ? 0 : s.getSixes(),
                    balls == 0 ? 0 : round2(runs * 100.0 / balls), status, onStrike));
        }

        List<BowlerLine> bowling = new ArrayList<>();
        for (Player p : playerRepo.findByTeamIdOrderByIdAsc(innings.getBowlingTeam().getId())) {
            PlayerMatchStats s = stats.get(p.getId());
            boolean current = !innings.isCompleted() && p.getId().equals(innings.getBowler().getId());
            if (s == null || (s.getBallsBowled() == 0 && !current)) {
                continue;
            }
            bowling.add(new BowlerLine(p.getId(), p.getName(), overs(s.getBallsBowled()),
                    s.getRunsConceded(), s.getWickets(),
                    s.getBallsBowled() == 0 ? 0 : round2(s.getRunsConceded() * 6.0 / s.getBallsBowled()),
                    current));
        }

        int balls = innings.getLegalBalls();
        double runRate = balls == 0 ? 0 : round2(innings.getRuns() * 6.0 / balls);

        Integer runsNeeded = null;
        Integer ballsLeft = null;
        Double requiredRate = null;
        if (innings.getTarget() != null && !innings.isCompleted()) {
            runsNeeded = Math.max(0, innings.getTarget() - innings.getRuns());
            ballsLeft = match.getTotalOvers() * 6 - balls;
            requiredRate = ballsLeft > 0 ? round2(runsNeeded * 6.0 / ballsLeft) : null;
        }

        return new InningsCard(innings.getInningsNumber(), innings.getBattingTeam().getName(),
                innings.getBowlingTeam().getName(), innings.getRuns(), innings.getWickets(), overs(balls),
                innings.getExtras(), runRate, innings.getTarget(), runsNeeded, ballsLeft, requiredRate,
                innings.isCompleted(), batting, bowling);
    }

    private BallDto toBallDto(BallEvent e) {
        return new BallDto(e.getInningsNumber(), e.getOverNumber() + "." + e.getBallNumber(), label(e),
                e.getRuns(), e.isWicket(), e.getExtraType().name(), e.getCommentary());
    }

    // short text shown in the little ball bubbles on the dashboard
    private String label(BallEvent e) {
        if (e.isWicket()) return "W";
        ExtraType extra = e.getExtraType();
        int runs = e.getRuns();
        return switch (extra) {
            case WIDE -> runs > 1 ? runs + "Wd" : "Wd";
            case NO_BALL -> runs > 1 ? (runs - 1) + "Nb" : "Nb";
            case BYE -> runs + "b";
            case LEG_BYE -> runs + "lb";
            case NONE -> String.valueOf(runs);
        };
    }

    static String overs(int legalBalls) {
        return (legalBalls / 6) + "." + (legalBalls % 6);
    }

    static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
