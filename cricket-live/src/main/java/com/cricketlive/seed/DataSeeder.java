package com.cricketlive.seed;

import com.cricketlive.dto.Dtos.CreateMatchRequest;
import com.cricketlive.feed.ScoreFeed;
import com.cricketlive.model.MatchStatus;
import com.cricketlive.model.Player;
import com.cricketlive.model.PlayerRole;
import com.cricketlive.model.Team;
import com.cricketlive.repository.PlayerRepository;
import com.cricketlive.repository.TeamRepository;
import com.cricketlive.service.MatchService;
import com.cricketlive.service.ScoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Fills an empty database with four teams and three demo matches (finished, live, upcoming). */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    // batting order: openers first, tail-enders last
    private static final PlayerRole[] ROLES = {
            PlayerRole.BATSMAN, PlayerRole.BATSMAN, PlayerRole.BATSMAN, PlayerRole.WICKET_KEEPER,
            PlayerRole.ALL_ROUNDER, PlayerRole.ALL_ROUNDER, PlayerRole.ALL_ROUNDER,
            PlayerRole.BOWLER, PlayerRole.BOWLER, PlayerRole.BOWLER, PlayerRole.BOWLER
    };

    private static final String[][] SQUADS = {
            {"Pune Panthers", "PNP",
                    "Aarav Deshpande", "Rohan Kulkarni", "Vihaan Joshi", "Ishaan Patwardhan", "Kabir Sathe",
                    "Aditya Bhosale", "Sahil Naik", "Omkar Pawar", "Tejas Ranade", "Yash Phadke", "Neel Apte"},
            {"Mumbai Mariners", "MUM",
                    "Arjun Mehra", "Dev Shetty", "Karan Lobo", "Rehan Sayyed", "Siddharth Rane",
                    "Nikhil Dsouza", "Varun Chavan", "Harsh Parab", "Manav Salvi", "Zaid Ansari", "Pratik Mhatre"},
            {"Delhi Dynamos", "DEL",
                    "Aman Verma", "Rishi Malhotra", "Tanmay Arora", "Kunal Bhatia", "Vivek Chauhan",
                    "Mohit Saxena", "Gaurav Tomar", "Lakshya Sehgal", "Parth Khanna", "Dhruv Bajaj", "Anuj Rawat"},
            {"Chennai Chargers", "CHN",
                    "Naveen Raghavan", "Surya Iyer", "Karthik Subramani", "Vignesh Pillai", "Hari Narayanan",
                    "Bala Murugan", "Dinesh Venkat", "Ashwin Raman", "Prabhu Selvam", "Gokul Krishnan", "Mani Shankar"}
    };

    private final TeamRepository teamRepo;
    private final PlayerRepository playerRepo;
    private final MatchService matchService;
    private final ScoringService scoringService;
    private final ScoreFeed feed;

    public DataSeeder(TeamRepository teamRepo, PlayerRepository playerRepo, MatchService matchService,
                      ScoringService scoringService, ScoreFeed feed) {
        this.teamRepo = teamRepo;
        this.playerRepo = playerRepo;
        this.matchService = matchService;
        this.scoringService = scoringService;
        this.feed = feed;
    }

    @Override
    public void run(String... args) {
        if (teamRepo.count() > 0) {
            return;
        }

        List<Team> teams = new ArrayList<>();
        for (String[] squad : SQUADS) {
            Team team = teamRepo.save(new Team(squad[0], squad[1]));
            for (int i = 0; i < ROLES.length; i++) {
                playerRepo.save(new Player(squad[i + 2], ROLES[i], team));
            }
            teams.add(team);
        }
        Team pune = teams.get(0);
        Team mumbai = teams.get(1);
        Team delhi = teams.get(2);
        Team chennai = teams.get(3);

        // 1) a finished 5-over match, played out instantly so the summary page has data
        Long finishedId = matchService.create(
                new CreateMatchRequest(delhi.getId(), chennai.getId(), "Capital Oval, Delhi", 5, false)).id();
        scoringService.startMatch(finishedId);
        MatchStatus status = MatchStatus.LIVE;
        for (int i = 0; i < 400 && status != MatchStatus.COMPLETED; i++) {
            status = scoringService.recordBall(finishedId, feed.nextDelivery());
        }

        // 2) a T20 that the simulator plays live
        Long liveId = matchService.create(
                new CreateMatchRequest(pune.getId(), mumbai.getId(), "Pune Premier Stadium", 20, true)).id();
        scoringService.startMatch(liveId);

        // 3) a fixture that has not started yet (start it from the dashboard)
        matchService.create(
                new CreateMatchRequest(mumbai.getId(), chennai.getId(), "Marine Drive Ground, Mumbai", 20, false));

        log.info("Demo data created: 4 teams, 3 matches");
    }
}
