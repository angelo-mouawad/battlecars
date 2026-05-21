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
    // Returns the created match with both players attached
    // -------------------------------------------------------------------------
    @Transactional
    public Match createMatch(Long player1Id, Integer car1Id, Long player2Id, Integer car2Id) {
        Player player1 = playerRepository.findById(player1Id)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + player1Id));
        Player player2 = playerRepository.findById(player2Id)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + player2Id));

        if (player1Id.equals(player2Id)) {
            throw new IllegalArgumentException("A player cannot play against themselves");
        }
        if (car1Id.equals(car2Id)) {
            throw new IllegalArgumentException("Both players cannot use the same car");
        }

        // Create the match in WAITING state
        Match match = new Match(Status.WAITING);
        match = matchRepository.save(match);

        // Create one MatchPlayer row per player — health defaults to 50 in MatchPlayer
        MatchPlayer mp1 = new MatchPlayer(match, player1, car1Id);
        MatchPlayer mp2 = new MatchPlayer(match, player2, car2Id);
        matchPlayerRepository.save(mp1);
        matchPlayerRepository.save(mp2);

        return match;
    }

    // -------------------------------------------------------------------------
    // START — called right after create, or can be a separate frontend action
    // Moves status from WAITING → ACTIVE and records start time
    // -------------------------------------------------------------------------
    @Transactional
    public Match startMatch(Long matchId) {
        Match match = getMatchOrThrow(matchId);

        if (match.getStatus() != Status.WAITING) {
            throw new IllegalStateException("Match is not in WAITING state, cannot start");
        }

        match.setStatus(Status.ACTIVE);
        match.setStartedAt(LocalDateTime.now());
        return matchRepository.save(match);
    }

    // -------------------------------------------------------------------------
    // PROCESS HIT — called by WebSocketHandler every time ESP32 sends hit:true
    // carId = the car that was hit
    // damage = 10 for now (light=5, medium=10, hard=15 later)
    // Returns a map with current health of both cars + whether match ended
    // -------------------------------------------------------------------------
    @Transactional
    public Map<String, Object> processHit(Long matchId, Integer carId, int damage) {
        Match match = getMatchOrThrow(matchId);

        if (match.getStatus() != Status.ACTIVE) {
            throw new IllegalStateException("Match is not active");
        }

        // Find the MatchPlayer for the car that was hit
        MatchPlayer hitPlayer = matchPlayerRepository.findByMatchIdAndCarId(matchId, carId)
                .orElseThrow(() -> new IllegalArgumentException("No player found for car " + carId + " in match " + matchId));

        // Apply damage
        hitPlayer.applyDamage(damage);
        matchPlayerRepository.save(hitPlayer);

        // Check if this car is now dead
        Map<String, Object> result = new HashMap<>();
        result.put("matchId", matchId);
        result.put("hitCarId", carId);
        result.put("damage", damage);
        result.put("remainingHealth", hitPlayer.getHealth());

        if (!hitPlayer.isAlive()) {
            // Find the other player — they are the winner
            List<MatchPlayer> allPlayers = matchPlayerRepository.findByMatchId(matchId);
            MatchPlayer winner = allPlayers.stream()
                    .filter(mp -> !mp.getCarId().equals(carId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Could not find winner"));

            endMatch(match, winner);
            result.put("matchOver", true);
            result.put("winnerId", winner.getPlayer().getId());
            result.put("winnerName", winner.getPlayer().getName());
        } else {
            result.put("matchOver", false);
        }

        // Also include full health state for both cars
        List<MatchPlayer> allPlayers = matchPlayerRepository.findByMatchId(matchId);
        for (MatchPlayer mp : allPlayers) {
            result.put("car" + mp.getCarId() + "Health", mp.getHealth());
        }

        return result;
    }

    // -------------------------------------------------------------------------
    // END MATCH — internal, called by processHit when a car reaches 0 HP
    // Also exposed publicly so frontend can force-end a match if needed
    // -------------------------------------------------------------------------
    @Transactional
    public Match endMatch(Long matchId, Long winnerPlayerId) {
        Match match = getMatchOrThrow(matchId);
        Player winner = playerRepository.findById(winnerPlayerId)
                .orElseThrow(() -> new IllegalArgumentException("Winner player not found: " + winnerPlayerId));
        return endMatch(match, matchPlayerRepository.findByMatchId(matchId).stream()
                .filter(mp -> mp.getPlayer().getId().equals(winnerPlayerId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Winner not in this match")));
    }

    // Internal version used by processHit (already has the MatchPlayer object)
    private Match endMatch(Match match, MatchPlayer winnerMatchPlayer) {
        match.setStatus(Status.FINISHED);
        match.setEndedAt(LocalDateTime.now());
        match.setWinner(winnerMatchPlayer.getPlayer());

        // Award +1 to winner's all-time score
        Player winner = winnerMatchPlayer.getPlayer();
        winner.setScore(winner.getScore() + 1);
        playerRepository.save(winner);

        return matchRepository.save(match);
    }

    // -------------------------------------------------------------------------
    // GET MATCH STATE — called by frontend to get current HP, status, players
    // Returns a clean map (avoids lazy loading issues with full entity)
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public Map<String, Object> getMatchState(Long matchId) {
        Match match = getMatchOrThrow(matchId);
        List<MatchPlayer> players = matchPlayerRepository.findByMatchId(matchId);

        Map<String, Object> state = new HashMap<>();
        state.put("matchId", match.getId());
        state.put("status", match.getStatus().name());
        state.put("startedAt", match.getStartedAt());
        state.put("endedAt", match.getEndedAt());

        if (match.getWinner() != null) {
            state.put("winnerId", match.getWinner().getId());
            state.put("winnerName", match.getWinner().getName());
        }

        // Build player state list: [{ playerId, playerName, carId, health, score }]
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
    // HISTORY — global match history + per-player history
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllFinishedMatches() {
        return matchRepository.findAll().stream()
                .filter(m -> m.getStatus() == Status.FINISHED)
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
    // ACTIVE MATCH — frontend calls this on page load to check if a match
    // is already running (e.g. after a refresh)
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public Map<String, Object> getActiveMatch() {
        return matchRepository.findFirstByStatus(Status.ACTIVE)
                .map(m -> getMatchState(m.getId()))
                .orElse(null);
    }

    // -------------------------------------------------------------------------
    // HELPERS
    // -------------------------------------------------------------------------
    private Match getMatchOrThrow(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
    }

    // Converts a Match entity into a clean summary map for history responses
    private Map<String, Object> matchToSummary(Match match) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("matchId", match.getId());
        summary.put("status", match.getStatus().name());
        summary.put("startedAt", match.getStartedAt());
        summary.put("endedAt", match.getEndedAt());

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