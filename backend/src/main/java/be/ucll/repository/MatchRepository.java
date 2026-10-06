package be.ucll.repository;

import be.ucll.model.Match;
import be.ucll.model.Match.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {

    // Find the first match with a given status, used to get the currently ACTIVE match
    Optional<Match> findFirstByStatus(Status status);

    // All matches a specific player was part of, newest first
    @Query("""
            SELECT DISTINCT m FROM Match m
            JOIN m.players mp
            WHERE mp.player.id = :playerId
            ORDER BY m.startedAt DESC
            """)
    List<Match> findMatchHistoryByPlayerId(@Param("playerId") Long playerId);

    // All FINISHED matches between two specific players, newest first
    @Query("""
            SELECT DISTINCT m FROM Match m
            WHERE m.status = 'FINISHED'
              AND EXISTS (
                  SELECT 1 FROM MatchPlayer mp1
                  WHERE mp1.match = m AND mp1.player.id = :player1Id
              )
              AND EXISTS (
                  SELECT 1 FROM MatchPlayer mp2
                  WHERE mp2.match = m AND mp2.player.id = :player2Id
              )
            ORDER BY m.startedAt DESC
            """)
    List<Match> findHeadToHeadHistory(
            @Param("player1Id") Long player1Id,
            @Param("player2Id") Long player2Id
    );
}