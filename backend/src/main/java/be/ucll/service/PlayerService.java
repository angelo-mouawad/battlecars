package be.ucll.service;

import java.util.List;

import org.springframework.stereotype.Service;

import be.ucll.model.Player;
import be.ucll.repository.PlayerRepository;

@Service
public class PlayerService {

    private final PlayerRepository repository;

    public PlayerService(PlayerRepository repository) {
        this.repository = repository;
    }

    public List<Player> getAll() {
        return repository.findAll();
    }

    public Player createPlayer(String name, String password) {
        Player player = new Player(name, password);
        return repository.save(player);
    }

    public Player changePassword(String name, String newPassword) {
        Player player = repository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Player not found: " + name));
        player.setPassword(newPassword);
        return repository.save(player);
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