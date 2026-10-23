package com.cricketlive.repository;

import com.cricketlive.model.BallEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BallEventRepository extends JpaRepository<BallEvent, Long> {

    List<BallEvent> findTop12ByMatchIdOrderByIdDesc(Long matchId);

    List<BallEvent> findByMatchIdOrderByIdDesc(Long matchId, Pageable pageable);
}
