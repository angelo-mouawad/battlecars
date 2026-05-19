package be.ucll.repository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import be.ucll.model.Player;

@Component
public class DbInitializer implements CommandLineRunner {

    private final PlayerRepository playerRepository;

    public DbInitializer(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Override
    public void run(String... args) {
        if (playerRepository.count() == 0) {
            playerRepository.save(new Player("Angelo", "moad123"));
            playerRepository.save(new Player("Mouad", "zabi"));
            playerRepository.save(new Player("Hunain", "hunain123"));
            playerRepository.save(new Player("Lama", "lama123"));
            playerRepository.save(new Player("Eddy", "eddy123"));
            playerRepository.save(new Player("Tugberk", "tugberk123"));
        }
    }
}