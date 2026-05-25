package be.ucll.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import be.ucll.model.Player;
import be.ucll.repository.PlayerRepository;

@Service
public class PlayerService {

    private final PlayerRepository repository;
    private final PasswordEncoder passwordEncoder;

    public PlayerService(PlayerRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Player> getAll() {
        return repository.findAll();
    }

    public Player createPlayer(String name, String password) {
        Player player = new Player(name, passwordEncoder.encode(password));
        return repository.save(player);
    }

    public Player changePassword(String name, String newPassword) {
        Player player = repository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + name));
        player.setPassword(passwordEncoder.encode(newPassword));
        return repository.save(player);
    }

    public boolean verifyPassword(String name, String rawPassword) {
        Player player = repository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + name));
        return passwordEncoder.matches(rawPassword, player.getPassword());
    }

    public Player addPoints(String name, int points) {
        Player player = repository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + name));
        player.setScore(player.getScore() + points);
        return repository.save(player);
    }

    public void deletePlayer(Long id) {
        Player player = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + id));
        repository.delete(player);
    }
}