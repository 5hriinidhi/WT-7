package com.cricketlive.repository;

import com.cricketlive.model.Innings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InningsRepository extends JpaRepository<Innings, Long> {

    List<Innings> findByMatchIdOrderByInningsNumber(Long matchId);

    List<Innings> findByMatchIdInOrderByInningsNumber(Collection<Long> matchIds);

    Optional<Innings> findByMatchIdAndInningsNumber(Long matchId, int inningsNumber);
}
