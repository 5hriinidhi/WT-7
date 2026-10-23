package com.cricketlive.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "cricket_matches", indexes = @Index(name = "idx_match_status", columnList = "status"))
public class CricketMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    // team1 always bats first (toss is not modelled)
    @ManyToOne(optional = false)
    @JoinColumn(name = "team1_id")
    private Team team1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "team2_id")
    private Team team2;

    @Column(nullable = false)
    private String venue;

    @Column(nullable = false)
    private int totalOvers;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.UPCOMING;

    private int currentInnings;

    private String result;

    // when true the built-in feed plays the match ball by ball
    private boolean simulated;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected CricketMatch() {
    }

    public CricketMatch(Team team1, Team team2, String venue, int totalOvers, boolean simulated) {
        this.team1 = team1;
        this.team2 = team2;
        this.venue = venue;
        this.totalOvers = totalOvers;
        this.simulated = simulated;
        this.title = team1.getName() + " vs " + team2.getName();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Team getTeam1() {
        return team1;
    }

    public Team getTeam2() {
        return team2;
    }

    public String getVenue() {
        return venue;
    }

    public int getTotalOvers() {
        return totalOvers;
    }

    public MatchStatus getStatus() {
        return status;
    }

    public void setStatus(MatchStatus status) {
        this.status = status;
    }

    public int getCurrentInnings() {
        return currentInnings;
    }

    public void setCurrentInnings(int currentInnings) {
        this.currentInnings = currentInnings;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public boolean isSimulated() {
        return simulated;
    }

    public void setSimulated(boolean simulated) {
        this.simulated = simulated;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
