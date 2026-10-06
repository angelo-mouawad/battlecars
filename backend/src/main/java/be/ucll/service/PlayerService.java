package be.ucll.service;

import be.ucll.exception.NotFoundException;

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

    public Player getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Player not found: " + id));
    }

    public Player createPlayer(String name, String password) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Player name is required");
        if (password == null || password.isBlank())
            throw new IllegalArgumentException("Password is required");
        String trimmed = name.trim();
        if (repository.findByName(trimmed).isPresent())
            throw new IllegalStateException("A player named " + trimmed + " already exists");
        Player player = new Player(trimmed, passwordEncoder.encode(password));
        return repository.save(player);
    }

    public Player changePassword(String name, String newPassword) {
        Player player = repository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Player not found: " + name));
        player.setPassword(passwordEncoder.encode(newPassword));
        return repository.save(player);
    }

    public boolean verifyPassword(String name, String rawPassword) {
        Player player = repository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Player not found: " + name));
        return rawPassword != null && passwordEncoder.matches(rawPassword, player.getPassword());
    }

    public Player addPoints(String name, int points) {
        Player player = repository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Player not found: " + name));
        player.setScore(player.getScore() + points);
        return repository.save(player);
    }

    public void deletePlayer(Long id) {
        Player player = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Player not found: " + id));
        repository.delete(player);
    }
}