package com.cricketlive.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** One delivery of the match, kept for ball-by-ball commentary. */
@Entity
@Table(name = "ball_events", indexes = @Index(name = "idx_ball_match", columnList = "match_id"))
public class BallEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private CricketMatch match;

    @Column(name = "innings_number", nullable = false)
    private int inningsNumber;

    // 0-based over index and the number of legal balls bowled in that over after this delivery
    private int overNumber;
    private int ballNumber;

    @ManyToOne(optional = false)
    private Player batter;

    @ManyToOne(optional = false)
    private Player bowler;

    // everything that went onto the total, including the wide / no-ball penalty
    private int runs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExtraType extraType;

    private boolean wicket;

    private String commentary;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected BallEvent() {
    }

    public BallEvent(CricketMatch match, int inningsNumber, int overNumber, int ballNumber,
                     Player batter, Player bowler, int runs, ExtraType extraType,
                     boolean wicket, String commentary) {
        this.match = match;
        this.inningsNumber = inningsNumber;
        this.overNumber = overNumber;
        this.ballNumber = ballNumber;
        this.batter = batter;
        this.bowler = bowler;
        this.runs = runs;
        this.extraType = extraType;
        this.wicket = wicket;
        this.commentary = commentary;
    }

    public Long getId() {
        return id;
    }

    public int getInningsNumber() {
        return inningsNumber;
    }

    public int getOverNumber() {
        return overNumber;
    }

    public int getBallNumber() {
        return ballNumber;
    }

    public Player getBatter() {
        return batter;
    }

    public Player getBowler() {
        return bowler;
    }

    public int getRuns() {
        return runs;
    }

    public ExtraType getExtraType() {
        return extraType;
    }

    public boolean isWicket() {
        return wicket;
    }

    public String getCommentary() {
        return commentary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
