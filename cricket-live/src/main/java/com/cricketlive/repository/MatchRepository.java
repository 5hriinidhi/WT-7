package com.cricketlive.repository;

import com.cricketlive.model.CricketMatch;
import com.cricketlive.model.MatchStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<CricketMatch, Long> {

    Page<CricketMatch> findByStatus(MatchStatus status, Pageable pageable);

    List<CricketMatch> findByStatusAndSimulatedTrue(MatchStatus status);

    // row lock so two scorers can never update the same match at the same moment
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from CricketMatch m where m.id = :id")
    Optional<CricketMatch> findByIdForUpdate(@Param("id") Long id);
}
