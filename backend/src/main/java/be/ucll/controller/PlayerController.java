package be.ucll.controller;

import java.util.List;

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

import be.ucll.controller.dto.LoginRequest;
import be.ucll.controller.dto.PasswordRequest;
import be.ucll.controller.dto.PlayerRequest;
import be.ucll.controller.dto.PointsRequest;
import be.ucll.exception.NotFoundException;
import be.ucll.model.Player;
import be.ucll.service.PlayerService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @GetMapping
    public ResponseEntity<List<Player>> getAllPlayers() {
        return ResponseEntity.ok(playerService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Player> getPlayer(@PathVariable Long id) {
        return ResponseEntity.ok(playerService.getById(id));
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody LoginRequest body) {
        if (body.name() == null || body.name().isBlank() || body.password() == null || body.password().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            boolean ok = playerService.verifyPassword(body.name(), body.password());
            return ok ? ResponseEntity.ok().build() : ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (NotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping
    public ResponseEntity<Player> createPlayer(@Valid @RequestBody PlayerRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playerService.createPlayer(body.name(), body.password()));
    }

    @PatchMapping("/{name}/password")
    public ResponseEntity<Player> changePassword(@PathVariable String name, @Valid @RequestBody PasswordRequest body) {
        return ResponseEntity.ok(playerService.changePassword(name, body.password()));
    }

    @PatchMapping("/{name}/score")
    public ResponseEntity<Player> addPoints(@PathVariable String name, @Valid @RequestBody PointsRequest body) {
        return ResponseEntity.ok(playerService.addPoints(name, body.points()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlayer(@PathVariable Long id) {
        playerService.deletePlayer(id);
        return ResponseEntity.noContent().build();
    }
}
