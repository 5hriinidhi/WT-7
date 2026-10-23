package com.cricketlive.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "innings",
        uniqueConstraints = @UniqueConstraint(name = "uk_innings_match_no", columnNames = {"match_id", "innings_number"}))
public class Innings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private CricketMatch match;

    @Column(name = "innings_number", nullable = false)
    private int inningsNumber;

    @ManyToOne(optional = false)
    private Team battingTeam;

    @ManyToOne(optional = false)
    private Team bowlingTeam;

    private int runs;
    private int wickets;

    // only legal deliveries count here, so overs = legalBalls / 6
    private int legalBalls;

    private int extras;

    private boolean completed;

    // set for the second innings only
    private Integer target;

    @ManyToOne
    private Player striker;

    @ManyToOne
    private Player nonStriker;

    @ManyToOne
    private Player bowler;

    // position in the batting order of the next batter to walk in
    private int nextBatterIndex;

    protected Innings() {
    }

    public Innings(CricketMatch match, int inningsNumber, Team battingTeam, Team bowlingTeam) {
        this.match = match;
        this.inningsNumber = inningsNumber;
        this.battingTeam = battingTeam;
        this.bowlingTeam = bowlingTeam;
    }

    public Long getId() {
        return id;
    }

    public CricketMatch getMatch() {
        return match;
    }

    public int getInningsNumber() {
        return inningsNumber;
    }

    public Team getBattingTeam() {
        return battingTeam;
    }

    public Team getBowlingTeam() {
        return bowlingTeam;
    }

    public int getRuns() {
        return runs;
    }

    public void setRuns(int runs) {
        this.runs = runs;
    }

    public int getWickets() {
        return wickets;
    }

    public void setWickets(int wickets) {
        this.wickets = wickets;
    }

    public int getLegalBalls() {
        return legalBalls;
    }

    public void setLegalBalls(int legalBalls) {
        this.legalBalls = legalBalls;
    }

    public int getExtras() {
        return extras;
    }

    public void setExtras(int extras) {
        this.extras = extras;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Integer getTarget() {
        return target;
    }

    public void setTarget(Integer target) {
        this.target = target;
    }

    public Player getStriker() {
        return striker;
    }

    public void setStriker(Player striker) {
        this.striker = striker;
    }

    public Player getNonStriker() {
        return nonStriker;
    }

    public void setNonStriker(Player nonStriker) {
        this.nonStriker = nonStriker;
    }

    public Player getBowler() {
        return bowler;
    }

    public void setBowler(Player bowler) {
        this.bowler = bowler;
    }

    public int getNextBatterIndex() {
        return nextBatterIndex;
    }

    public void setNextBatterIndex(int nextBatterIndex) {
        this.nextBatterIndex = nextBatterIndex;
    }
}
