package com.cricketlive.repository;

import com.cricketlive.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    // squad in batting order (ids are assigned in the order players were created)
    List<Player> findByTeamIdOrderByIdAsc(Long teamId);

    long countByTeamId(Long teamId);

    List<Player> findAllByOrderByNameAsc();

    List<Player> findByTeamIdOrderByNameAsc(Long teamId);

    List<Player> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    List<Player> findByTeamIdAndNameContainingIgnoreCaseOrderByNameAsc(Long teamId, String name);
}
