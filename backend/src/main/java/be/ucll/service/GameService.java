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

        // Block if a match is already running or waiting
        matchRepository.findFirstByStatus(Status.ACTIVE).ifPresent(m -> {
            throw new IllegalStateException("Match " + m.getId() + " is already active. Finish it first.");
        });
        matchRepository.findFirstByStatus(Status.WAITING).ifPresent(m -> {
            throw new IllegalStateException("Match " + m.getId() + " is already waiting to start. Start or cancel it first.");
        });

        Match match = new Match(Status.WAITING);
        match = matchRepository.save(match);

        matchPlayerRepository.save(new MatchPlayer(match, player1, car1Id));
        matchPlayerRepository.save(new MatchPlayer(match, player2, car2Id));

        Map<String, Object> result = new HashMap<>();
        result.put("matchId", match.getId());
        result.put("status", match.getStatus().name());
        result.put("player1", Map.of("id", player1.getId(), "name", player1.getName(), "carId", car1Id, "health", MatchPlayer.MAX_HEALTH));
        result.put("player2", Map.of("id", player2.getId(), "name", player2.getName(), "carId", car2Id, "health", MatchPlayer.MAX_HEALTH));
        return result;
    }

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

    @Transactional
    public Map<String, Object> processHit(Long matchId, Integer carId, int damage) {
        Match match = getMatchOrThrow(matchId);

        if (match.getStatus() != Status.ACTIVE)
            throw new IllegalStateException("Match is not active, cannot process hit");

        // Both players lose HP on every hit
        // TODO: replace with directional logic when electronics team delivers it
        //   front-front → both lose HP | side hit → only hit car loses HP
        List<MatchPlayer> allPlayers = matchPlayerRepository.findByMatchId(matchId);

        for (MatchPlayer mp : allPlayers) {
            mp.applyDamage(damage);
            matchPlayerRepository.save(mp);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("type", "HIT_RESULT");
        result.put("matchId", matchId);
        result.put("hitCarId", carId);
        result.put("damage", damage);

        for (MatchPlayer mp : allPlayers) {
            result.put("car" + mp.getCarId() + "Health", mp.getHealth());
        }

        MatchPlayer dead = allPlayers.stream().filter(mp -> !mp.isAlive()).findFirst().orElse(null);

        if (dead != null) {
            MatchPlayer winner = allPlayers.stream()
                    .filter(MatchPlayer::isAlive)
                    .findFirst()
                    .orElse(null); // null = draw (both hit 0 on the same hit)

            finishMatch(match, winner);

            result.put("matchOver", true);
            if (winner != null) {
                result.put("winnerId", winner.getPlayer().getId());
                result.put("winnerName", winner.getPlayer().getName());
                result.put("winnerCarId", winner.getCarId());
            } else {
                result.put("draw", true);
            }
        } else {
            result.put("matchOver", false);
        }

        return result;
    }

    @Transactional
    public Map<String, Object> endMatch(Long matchId, Long winnerPlayerId) {
        Match match = getMatchOrThrow(matchId);

        MatchPlayer winnerMp = matchPlayerRepository.findByMatchIdAndPlayerId(matchId, winnerPlayerId)
                .orElseThrow(() -> new IllegalStateException("Winner not found in this match"));

        finishMatch(match, winnerMp);
        return getMatchState(matchId);
    }

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

    @Transactional(readOnly = true)
    public Map<String, Object> getActiveMatch() {
        return matchRepository.findFirstByStatus(Status.ACTIVE)
                .map(m -> getMatchState(m.getId()))
                .orElse(null);
    }

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

    private Match getMatchOrThrow(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found: " + matchId));
    }

    // winnerMatchPlayer is null in a draw — no winner set, no points awarded
    private void finishMatch(Match match, MatchPlayer winnerMatchPlayer) {
        match.setStatus(Status.FINISHED);
        match.setEndedAt(LocalDateTime.now());

        if (winnerMatchPlayer != null) {
            match.setWinner(winnerMatchPlayer.getPlayer());
            matchRepository.save(match);

            Player winner = winnerMatchPlayer.getPlayer();
            winner.setScore(winner.getScore() + 1);
            playerRepository.save(winner);
        } else {
            matchRepository.save(match);
        }
    }

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
