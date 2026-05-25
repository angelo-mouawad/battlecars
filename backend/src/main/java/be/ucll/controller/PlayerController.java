package be.ucll.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import be.ucll.model.Player;
import be.ucll.service.PlayerService;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    // GET /api/players
    @GetMapping
    public ResponseEntity<List<Player>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getAll());
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody Map<String, String> body) {
        boolean ok = playerService.verifyPassword(body.get("name"), body.get("password"));
        return ok ? ResponseEntity.ok().build()
                  : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping
    public ResponseEntity<Player> createPlayer(@RequestBody Map<String, String> body) {
        Player created = playerService.createPlayer(body.get("name"), body.get("password"));
        return ResponseEntity.ok(created);
    }

    // PATCH /api/players/{name}/password
    // Body: { "password": "..." }
    @PatchMapping("/{name}/password")
    public ResponseEntity<Player> changePassword(@PathVariable String name,
                                                 @RequestBody Map<String, String> body) {
        Player updated = playerService.changePassword(name, body.get("password"));
        return ResponseEntity.ok(updated);
    }

    // PATCH /api/players/{name}/score
    // Body: { "points": 10 }
    @PatchMapping("/{name}/score")
    public ResponseEntity<Player> addPoints(@PathVariable String name,
                                            @RequestBody Map<String, Integer> body) {
        Player updated = playerService.addPoints(name, body.get("points"));
        return ResponseEntity.ok(updated);
    }

    // DELETE /api/players/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}