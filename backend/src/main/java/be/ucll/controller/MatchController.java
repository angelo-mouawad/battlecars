package be.ucll.controller;

import be.ucll.service.CommentaryService;
import be.ucll.service.GameService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final GameService gameService;

    @Autowired
    private CommentaryService commentaryService;

    public MatchController(GameService gameService) {
        this.gameService = gameService;
    }

    // POST /api/matches
    // Called from lobby (or play page) when both players are ready
    // Body: { "player1Id": 1, "car1Id": 1, "player2Id": 2, "car2Id": 2 }
    // Returns: { matchId, status, player1: {...}, player2: {...} }
    @PostMapping
    public ResponseEntity<Map<String, Object>> createMatch(@RequestBody Map<String, Integer> body) {
        Map<String, Object> result = gameService.createMatch(
                body.get("player1Id").longValue(),
                body.get("car1Id"),
                body.get("player2Id").longValue(),
                body.get("car2Id")
        );
        return ResponseEntity.ok(result);
    }

    // POST /api/matches/{id}/start
    // Moves the match from WAITING → ACTIVE
    // Frontend calls this right after createMatch
    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startMatch(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.startMatch(id));
    }

    // GET /api/matches/{id}
    // Returns full match state: status, both players with health, winner if finished
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMatchState(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.getMatchState(id));
    }

    // GET /api/matches/active
    // Returns the currently ACTIVE match, or 404 if none running
    // Frontend calls this on play page load to recover after a browser refresh
    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveMatch() {
        Map<String, Object> active = gameService.getActiveMatch();
        if (active == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(active);
    }

    // POST /api/matches/{id}/hit
    // Applies damage to a car — used for testing without the ESP32
    // The WebSocket handler calls gameService.processHit() directly in production
    // Body: { "carId": 1, "damage": 10 }
    @PostMapping("/{id}/hit")
    public ResponseEntity<Map<String, Object>> processHit(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        int damage = body.getOrDefault("damage", 10);
        return ResponseEntity.ok(gameService.processHit(id, body.get("carId"), damage));
    }

    // POST /api/matches/{id}/end
    // Force-end a match — used if a player quits or something goes wrong
    // Body: { "winnerPlayerId": 1 }
    @PostMapping("/{id}/end")
    public ResponseEntity<Map<String, Object>> endMatch(
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
    // All finished matches for a specific player — used for per-player history
    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<Map<String, Object>>> getPlayerHistory(@PathVariable Long playerId) {
        return ResponseEntity.ok(gameService.getMatchHistoryByPlayer(playerId));
    }

    // GET /api/matches/h2h?player1=1&player2=2
    // All finished matches between two specific players
    @GetMapping("/h2h")
    public ResponseEntity<List<Map<String, Object>>> getHeadToHead(
            @RequestParam Long player1,
            @RequestParam Long player2) {
        return ResponseEntity.ok(gameService.getHeadToHeadHistory(player1, player2));
    }

    @GetMapping("/test/commentary")
    public Map<String, Object> testCommentary() throws Exception {
        Map<String, Object> fakeHit = new HashMap<>();
        fakeHit.put("matchOver", false);
        fakeHit.put("damage", 15);
        fakeHit.put("hitCarId", 1);

        String result = commentaryService
            .generateCommentary(fakeHit, "eddy", 35, "Zenin", 20)
            .get();

        return Map.of("commentary", result !=null ? result : "generation failed");    

    }
}