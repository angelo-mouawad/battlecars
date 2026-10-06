package be.ucll.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import be.ucll.controller.dto.CreateMatchRequest;
import be.ucll.controller.dto.EndMatchRequest;
import be.ucll.controller.dto.HitRequest;
import be.ucll.service.GameService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final GameService gameService;

    public MatchController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createMatch(@Valid @RequestBody CreateMatchRequest body) {
        return ResponseEntity.ok(gameService.createMatch(body.player1Id(), body.car1Id(), body.player2Id(), body.car2Id()));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> startMatch(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.startMatch(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMatchState(@PathVariable Long id) {
        return ResponseEntity.ok(gameService.getMatchState(id));
    }

    @GetMapping("/active")
    public ResponseEntity<Map<String, Object>> getActiveMatch() {
        Map<String, Object> active = gameService.getActiveMatch();
        return active == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(active);
    }

    @PostMapping("/{id}/hit")
    public ResponseEntity<Map<String, Object>> processHit(@PathVariable Long id, @Valid @RequestBody HitRequest body) {
        return ResponseEntity.ok(gameService.processHit(id, body.carId(), body.damageOrDefault()));
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<Map<String, Object>> endMatch(@PathVariable Long id, @Valid @RequestBody EndMatchRequest body) {
        return ResponseEntity.ok(gameService.endMatch(id, body.winnerPlayerId()));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getAllMatches() {
        return ResponseEntity.ok(gameService.getAllFinishedMatches());
    }

    @GetMapping("/player/{playerId}")
    public ResponseEntity<List<Map<String, Object>>> getPlayerHistory(@PathVariable Long playerId) {
        return ResponseEntity.ok(gameService.getMatchHistoryByPlayer(playerId));
    }

    @GetMapping("/h2h")
    public ResponseEntity<List<Map<String, Object>>> getHeadToHead(@RequestParam Long player1, @RequestParam Long player2) {
        return ResponseEntity.ok(gameService.getHeadToHeadHistory(player1, player2));
    }
}
