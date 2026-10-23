package com.cricketlive.feed;

import com.cricketlive.dto.Dtos.BallRequest;

/**
 * Where deliveries come from. The simulator below is the default; to use a real
 * cricket data provider, write another implementation that calls its API and
 * maps each delivery to a BallRequest.
 */
public interface ScoreFeed {

    BallRequest nextDelivery();
}
