package be.ucll.service;

import be.ucll.model.Match;
import be.ucll.model.Match.Status;
import be.ucll.model.MatchPlayer;
import be.ucll.model.Player;
import be.ucll.repository.MatchPlayerRepository;
import be.ucll.repository.MatchRepository;
import be.ucll.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GameService {

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final PlayerRepository playerRepository;

    public GameService(MatchRepository matchRepository,
                       MatchPlayerRepository matchPlayerRepository,
                       PlayerRepository playerRepository) {
        this.matchRepository = matchRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.playerRepository = playerRepository;
    }

    // -------------------------------------------------------------------------
    // CREATE — called from lobby when both players click Play
    // Body: { player1Id, car1Id, player2Id, car2Id }
    // Creates a match in WAITING state with 2 match_player rows (health = 50 each)
    // Returns the match ID — frontend stores this and sends MATCH_START:{id} over WS
    // -------------------------------------------------------------------------
    @Transactional
    public Map<String, Object> createMatch(Long player1Id, Integer car1Id,
                                           Long player2Id, Integer car2Id) {
        Player player1 = playerRepository.findById(player1Id)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + player1Id));
        Player player2 = playerRepository.findById(player2Id)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + player2Id));

        if (player1Id.equals(player2Id))
            throw new IllegalArgumentException("A player cannot play against themselves");
        if (car1Id.equals(car2Id))
            throw new IllegalArgumentException("Both players cannot use the same car");

        Match match = new Match(Status.WAITING);
        match = matchRepository.save(match);

        matchPlayerRepository.save(new MatchPlayer(match, player1, car1Id));
        matchPlayerRepository.save(new MatchPlayer(match, player2, car2Id));

        // Return a simple map — avoids lazy loading issues with the full Match entity
        Map<String, Object> result = new HashMap<>();
        result.put("matchId", match.getId());
        result.put("status", match.getStatus().name());
        result.put("player1", Map.of("id", player1.getId(), "name", player1.getName(), "carId", car1Id, "health", MatchPlayer.MAX_HEALTH));
        result.put("player2", Map.of("id", player2.getId(), "name", player2.getName(), "carId", car2Id, "health", MatchPlayer.MAX_HEALTH));
        return result;
    }

    // -------------------------------------------------------------------------
    // START — moves WAITING → ACTIVE
    // Called right after createMatch — or could be a separate "Ready" button later
    // -------------------------------------------------------------------------
    @Transactional
    public Map<String, Object> startMatch(Long matchId) {
        Match match = getMatchOrThrow(matchId);

        if (match.getStatus() != Status.WAITING)
            throw new IllegalStateException("Match is not in WAITING state, cannot start");

        match.setStatus(Status.ACTIVE);
        match.setStartedAt(LocalDateTime.now());
        matchRepository.save(match);

        return getMatchState(matchId);
    }

    // -------------------------------------------------------------------------
    // PROCESS HIT — called by BattleCarsWebSocketHandler every time ESP32 sends hit:true
    //
    // carId   = the physical car that was hit (1 or 2)
    // damage  = 10 by default
    //           5  if hitStrength = "light"
    //           10 if hitStrength = "medium" or missing
    //           15 if hitStrength = "hard"
    //
    // Returns: current health of both cars + matchOver flag + winner info if match ended
    // This result is broadcast back to all browsers over WebSocket as a HIT_RESULT message
    // -------------------------------------------------------------------------
    @Transactional
    public Map<String, Object> processHit(Long matchId, Integer carId, int damage) {
        Match match = getMatchOrThrow(matchId);

        if (match.getStatus() != Status.ACTIVE)
            throw new IllegalStateException("Match is not active, cannot process hit");

        // Find the MatchPlayer entry for the car that was hit
        MatchPlayer hitPlayer = matchPlayerRepository.findByMatchIdAndCarId(matchId, carId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No player found for car " + carId + " in match " + matchId));

        hitPlayer.applyDamage(damage);
        matchPlayerRepository.save(hitPlayer);

        // Get both players to build the full health state
        List<MatchPlayer> allPlayers = matchPlayerRepository.findByMatchId(matchId);

        Map<String, Object> result = new HashMap<>();
        result.put("type", "HIT_RESULT");
        result.put("matchId", matchId);
        result.put("hitCarId", carId);
        result.put("damage", damage);

        // Include health for every car in the match
        for (MatchPlayer mp : allPlayers) {
            result.put("car" + mp.getCarId() + "Health", mp.getHealth());
        }

        if (!hitPlayer.isAlive()) {
            // The car that was hit is dead — the other player wins
            MatchPlayer winner = allPlayers.stream()
                    .filter(mp -> !mp.getCarId().equals(carId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not determine winner"));

            finishMatch(match, winner);

            result.put("matchOver", true);
            result.put("winnerId", winner.getPlayer().getId());
            result.put("winnerName", winner.getPlayer().getName());
            result.put("winnerCarId", winner.getCarId());
        } else {
            result.put("matchOver", false);
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // END MATCH (manual) — force-end a match, e.g. if a player disconnects
    // Body: { winnerPlayerId }
    // -------------------------------------------------------------------------
    @Transactional
    public Map<String, Object> endMatch(Long matchId, Long winnerPlayerId) {
        Match match = getMatchOrThrow(matchId);

        MatchPlayer winnerMp = matchPlayerRepository.findByMatchIdAndPlayerId(matchId, winnerPlayerId)
                .orElseThrow(() -> new IllegalStateException("Winner not found in this match"));

        finishMatch(match, winnerMp);
        return getMatchState(matchId);
    }

    // -------------------------------------------------------------------------
    // GET MATCH STATE — returns full current state of a match
    // Used by frontend on page load to recover after a refresh
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public Map<String, Object> getMatchState(Long matchId) {
        Match match = getMatchOrThrow(matchId);
        List<MatchPlayer> players = matchPlayerRepository.findByMatchId(matchId);

        Map<String, Object> state = new HashMap<>();
        state.put("matchId", match.getId());
        state.put("status", match.getStatus().name());
        state.put("startedAt", match.getStartedAt() != null ? match.getStartedAt().toString() : null);
        state.put("endedAt", match.getEndedAt() != null ? match.getEndedAt().toString() : null);

        if (match.getWinner() != null) {
            state.put("winnerId", match.getWinner().getId());
            state.put("winnerName", match.getWinner().getName());
        }

        List<Map<String, Object>> playerStates = players.stream().map(mp -> {
            Map<String, Object> ps = new HashMap<>();
            ps.put("playerId", mp.getPlayer().getId());
            ps.put("playerName", mp.getPlayer().getName());
            ps.put("carId", mp.getCarId());
            ps.put("health", mp.getHealth());
            ps.put("score", mp.getScore());
            return ps;
        }).collect(Collectors.toList());

        state.put("players", playerStates);
        return state;
    }

    // -------------------------------------------------------------------------
    // GET ACTIVE MATCH — returns the currently ACTIVE match or null
    // Frontend calls this on play page load to reconnect after a refresh
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public Map<String, Object> getActiveMatch() {
        return matchRepository.findFirstByStatus(Status.ACTIVE)
                .map(m -> getMatchState(m.getId()))
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // HISTORY — global + per-player + head-to-head
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllFinishedMatches() {
        return matchRepository.findAll().stream()
                .filter(m -> m.getStatus() == Status.FINISHED)
                .sorted((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()))
                .map(this::matchToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMatchHistoryByPlayer(Long playerId) {
        return matchRepository.findMatchHistoryByPlayerId(playerId).stream()
                .filter(m -> m.getStatus() == Status.FINISHED)
                .map(this::matchToSummary)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getHeadToHeadHistory(Long player1Id, Long player2Id) {
        return matchRepository.findHeadToHeadHistory(player1Id, player2Id).stream()
                .map(this::matchToSummary)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    private Match getMatchOrThrow(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
    }

    // Core finish logic — sets status, records end time, sets winner, awards +1 point
    private void finishMatch(Match match, MatchPlayer winnerMatchPlayer) {
        match.setStatus(Status.FINISHED);
        match.setEndedAt(LocalDateTime.now());
        match.setWinner(winnerMatchPlayer.getPlayer());
        matchRepository.save(match);

        // Award +1 to winner's all-time score on the players table
        Player winner = winnerMatchPlayer.getPlayer();
        winner.setScore(winner.getScore() + 1);
        playerRepository.save(winner);
    }

    // Converts a Match to a clean summary map — used for history responses
    private Map<String, Object> matchToSummary(Match match) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("matchId", match.getId());
        summary.put("status", match.getStatus().name());
        summary.put("startedAt", match.getStartedAt() != null ? match.getStartedAt().toString() : null);
        summary.put("endedAt", match.getEndedAt() != null ? match.getEndedAt().toString() : null);

        if (match.getWinner() != null) {
            summary.put("winnerId", match.getWinner().getId());
            summary.put("winnerName", match.getWinner().getName());
        }

        List<MatchPlayer> players = matchPlayerRepository.findByMatchId(match.getId());
        List<Map<String, Object>> playerSummaries = players.stream().map(mp -> {
            Map<String, Object> ps = new HashMap<>();
            ps.put("playerId", mp.getPlayer().getId());
            ps.put("playerName", mp.getPlayer().getName());
            ps.put("carId", mp.getCarId());
            ps.put("health", mp.getHealth());
            return ps;
        }).collect(Collectors.toList());

        summary.put("players", playerSummaries);
        return summary;
    }
}