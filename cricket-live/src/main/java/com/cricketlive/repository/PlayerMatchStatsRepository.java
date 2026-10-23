package com.cricketlive.repository;

import com.cricketlive.model.PlayerMatchStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlayerMatchStatsRepository extends JpaRepository<PlayerMatchStats, Long> {

    Optional<PlayerMatchStats> findByMatchIdAndPlayerId(Long matchId, Long playerId);

    List<PlayerMatchStats> findByMatchId(Long matchId);

    List<PlayerMatchStats> findByPlayerId(Long playerId);

    // each row: playerId, name, team short name, total runs
    @Query("""
            select p.id, p.name, t.shortName, sum(s.runs)
            from PlayerMatchStats s join s.player p join p.team t
            group by p.id, p.name, t.shortName
            having sum(s.runs) > 0
            order by sum(s.runs) desc, p.name asc
            """)
    List<Object[]> topScorers(Pageable pageable);

    // each row: playerId, name, team short name, total wickets
    @Query("""
            select p.id, p.name, t.shortName, sum(s.wickets)
            from PlayerMatchStats s join s.player p join p.team t
            group by p.id, p.name, t.shortName
            having sum(s.wickets) > 0
            order by sum(s.wickets) desc, p.name asc
            """)
    List<Object[]> topWicketTakers(Pageable pageable);
}
