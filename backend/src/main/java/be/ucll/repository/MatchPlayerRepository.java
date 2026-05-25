package be.ucll.repository;

import be.ucll.model.MatchPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchPlayerRepository extends JpaRepository<MatchPlayer, Long> {

    // All participants in a given match (always 2)
    List<MatchPlayer> findByMatchId(Long matchId);

    // Find a specific player's entry in a specific match
    Optional<MatchPlayer> findByMatchIdAndPlayerId(Long matchId, Long playerId);

    // Find a player by which car they are driving in a given match
    Optional<MatchPlayer> findByMatchIdAndCarId(Long matchId, Integer carId);
}