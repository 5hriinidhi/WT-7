package com.cricketlive;

import com.cricketlive.dto.Dtos.BatterLine;
import com.cricketlive.dto.Dtos.BowlerLine;
import com.cricketlive.dto.Dtos.CreateMatchRequest;
import com.cricketlive.dto.Dtos.InningsCard;
import com.cricketlive.dto.Dtos.MatchCard;
import com.cricketlive.dto.Dtos.Scoreboard;
import com.cricketlive.feed.ScoreFeed;
import com.cricketlive.model.MatchStatus;
import com.cricketlive.model.Team;
import com.cricketlive.repository.TeamRepository;
import com.cricketlive.service.MatchService;
import com.cricketlive.service.ScoreboardService;
import com.cricketlive.service.ScoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "cricket.simulator.enabled=false")
@ActiveProfiles("h2")
@AutoConfigureMockMvc
class CricketFlowTest {

    @Autowired
    MatchService matchService;
    @Autowired
    ScoringService scoring;
    @Autowired
    ScoreboardService scoreboards;
    @Autowired
    ScoreFeed feed;
    @Autowired
    TeamRepository teams;
    @Autowired
    MockMvc mvc;

    private MatchCard newMatch(int overs) {
        List<Team> all = teams.findAll();
        return matchService.create(
                new CreateMatchRequest(all.get(0).getId(), all.get(1).getId(), "Test Ground", overs, false));
    }

    @Test
    void aWholeMatchEndsWithAResultAndTheNumbersAddUp() {
        MatchCard card = newMatch(3);
        scoring.startMatch(card.id());

        MatchStatus state = MatchStatus.LIVE;
        for (int i = 0; i < 500 && state != MatchStatus.COMPLETED; i++) {
            state = scoring.recordBall(card.id(), feed.nextDelivery());
        }
        assertEquals(MatchStatus.COMPLETED, state);

        Scoreboard board = scoreboards.scoreboard(card.id());
        assertNotNull(board.result());
        assertEquals(2, board.innings().size());

        for (InningsCard innings : board.innings()) {
            int fromBats = innings.batting().stream().mapToInt(BatterLine::runs).sum();
            assertEquals(innings.runs(), fromBats + innings.extras(), "batters + extras = total");

            int fromBowlers = innings.bowling().stream().mapToInt(BowlerLine::wickets).sum();
            assertEquals(innings.wickets(), fromBowlers, "bowlers' wickets = wickets that fell");
        }
    }

    @Test
    void demoDataIsThereAndOneMatchIsLive() throws Exception {
        mvc.perform(get("/api/teams")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(4));
        mvc.perform(get("/api/matches").param("status", "LIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("LIVE"));
    }

    @Test
    void unknownMatchGives404() throws Exception {
        mvc.perform(get("/api/matches/999999")).andExpect(status().isNotFound());
    }

    @Test
    void cannotScoreAMatchThatHasNotStarted() throws Exception {
        MatchCard card = newMatch(5);
        mvc.perform(post("/api/matches/{id}/balls", card.id())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"runs\":1}"))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsImpossibleRuns() throws Exception {
        MatchCard card = newMatch(5);
        scoring.startMatch(card.id());
        mvc.perform(post("/api/matches/{id}/balls", card.id())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"runs\":9}"))
                .andExpect(status().isBadRequest());
    }
}
