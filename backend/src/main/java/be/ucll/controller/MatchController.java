package be.ucll.controller;

import be.ucll.model.Match;
import be.ucll.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final GameService gameService;

    public MatchController(GameService gameService) {
        this.gameService = gameService;
    }

    // POST /api/matches
    // Called from lobby when both players click Play
    // Body: { "player1Id": 1, "car1Id": 1, "player2Id": 2, "car2Id": 2 }
    // Returns the created match (status: WAITING)
    @PostMapping
    public ResponseEntity<Match> createMatch(@RequestBody Map<String, Integer> body) {
        Match match = gameService.createMatch(
                body.get("player1Id").longValue(),
                body.get("car1Id"),
                body.get("player2Id").longValue(),
                body.get("car2Id")
        );
        return ResponseEntity.ok(match);
    }

    // POST /api/matches/{id}/start
    // Called right after create — moves WAITING → ACTIVE
    @PostMapping("/{id}/start")
    public ResponseEntity<Match> startMatch(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.startMatch(id));
    }

    // GET /api/matches/{id}
    // Returns full match state: status, both players, health, winner
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMatchState(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.getMatchState(id));
    }

    // GET /api/matches/active
    // Returns the currently ACTIVE match, or 404 if none
    // Frontend calls this on play page load to recover after a refresh
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveMatch() {
        Map<String, Object> active = gameService.getActiveMatch();
        if (active == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(active);
    }

    // POST /api/matches/{id}/hit
    // Called by WebSocketHandler internally — or directly for testing
    // Body: { "carId": 1, "damage": 10 }
    // Returns updated health of both cars + matchOver flag
    @PostMapping("/{id}/hit")
    public ResponseEntity<Map<String, Object>> processHit(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        int damage = body.getOrDefault("damage", 10); // default 10 if not provided
        Map<String, Object> result = gameService.processHit(id, body.get("carId"), damage);
        return ResponseEntity.ok(result);
    }

    // POST /api/matches/{id}/end
    // Force-end a match — used if something goes wrong or a player rage-quits
    // Body: { "winnerPlayerId": 1 }
    @PostMapping("/{id}/end")
    public ResponseEntity<Match> endMatch(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(gameService.endMatch(id, body.get("winnerPlayerId")));
    }

    // GET /api/matches
    // All finished matches — used for global match history panel
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllMatches() {
        return ResponseEntity.ok(gameService.getAllFinishedMatches());
    }

    // GET /api/matches/player/{playerId}
    // Match history for a specific player
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<Map<String, Object>>> getPlayerHistory(@PathVariable Long playerId) {
        return ResponseEntity.ok(gameService.getMatchHistoryByPlayer(playerId));
    }

    // GET /api/matches/h2h?player1=1&player2=2
    // Head-to-head history between two specific players
    @GetMapping("/h2h")
    public ResponseEntity<List<Map<String, Object>>> getHeadToHead(
            @RequestParam Long player1,
            @RequestParam Long player2) {
        return ResponseEntity.ok(gameService.getHeadToHeadHistory(player1, player2));
    }
}