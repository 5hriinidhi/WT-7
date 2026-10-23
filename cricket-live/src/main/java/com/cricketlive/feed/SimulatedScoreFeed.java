package com.cricketlive.feed;

import com.cricketlive.dto.Dtos.BallRequest;
import com.cricketlive.model.ExtraType;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/** Produces T20-like outcomes: mostly singles and dots, the odd boundary or wicket. */
@Component
public class SimulatedScoreFeed implements ScoreFeed {

    @Override
    public BallRequest nextDelivery() {
        int roll = ThreadLocalRandom.current().nextInt(100);

        if (roll < 33) return new BallRequest(0, ExtraType.NONE, false);
        if (roll < 63) return new BallRequest(1, ExtraType.NONE, false);
        if (roll < 70) return new BallRequest(2, ExtraType.NONE, false);
        if (roll < 71) return new BallRequest(3, ExtraType.NONE, false);
        if (roll < 82) return new BallRequest(4, ExtraType.NONE, false);
        if (roll < 88) return new BallRequest(6, ExtraType.NONE, false);
        if (roll < 93) return new BallRequest(0, ExtraType.NONE, true);
        if (roll < 96) return new BallRequest(0, ExtraType.WIDE, false);
        if (roll < 97) return new BallRequest(0, ExtraType.NO_BALL, false);
        if (roll < 98) return new BallRequest(1, ExtraType.BYE, false);
        return new BallRequest(1, ExtraType.LEG_BYE, false);
    }
}
