package com.cricketlive.model;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Batting and bowling figures of one player in one match. */
@Entity
@Table(name = "player_match_stats",
        uniqueConstraints = @UniqueConstraint(name = "uk_stats_match_player", columnNames = {"match_id", "player_id"}))
public class PlayerMatchStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private CricketMatch match;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id")
    private Player player;

    // batting
    private int runs;
    private int ballsFaced;
    private int fours;
    private int sixes;
    private boolean dismissed;

    // bowling
    private int ballsBowled;
    private int runsConceded;
    private int wickets;

    protected PlayerMatchStats() {
    }

    public PlayerMatchStats(CricketMatch match, Player player) {
        this.match = match;
        this.player = player;
    }

    public Long getId() {
        return id;
    }

    public CricketMatch getMatch() {
        return match;
    }

    public Player getPlayer() {
        return player;
    }

    public int getRuns() {
        return runs;
    }

    public void setRuns(int runs) {
        this.runs = runs;
    }

    public int getBallsFaced() {
        return ballsFaced;
    }

    public void setBallsFaced(int ballsFaced) {
        this.ballsFaced = ballsFaced;
    }

    public int getFours() {
        return fours;
    }

    public void setFours(int fours) {
        this.fours = fours;
    }

    public int getSixes() {
        return sixes;
    }

    public void setSixes(int sixes) {
        this.sixes = sixes;
    }

    public boolean isDismissed() {
        return dismissed;
    }

    public void setDismissed(boolean dismissed) {
        this.dismissed = dismissed;
    }

    public int getBallsBowled() {
        return ballsBowled;
    }

    public void setBallsBowled(int ballsBowled) {
        this.ballsBowled = ballsBowled;
    }

    public int getRunsConceded() {
        return runsConceded;
    }

    public void setRunsConceded(int runsConceded) {
        this.runsConceded = runsConceded;
    }

    public int getWickets() {
        return wickets;
    }

    public void setWickets(int wickets) {
        this.wickets = wickets;
    }
}
