package be.ucll.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import be.ucll.model.Player;

/**
 * Seeds six demo players into an empty database when SEED_DEMO_PLAYERS is true.
 * Their passwords are public in the README, so leave it off on anything reachable from the internet.
 */
@Component
public class DbInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DbInitializer.class);

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedDemoPlayers;

    public DbInitializer(PlayerRepository playerRepository, PasswordEncoder passwordEncoder,
                         @Value("${battlecars.seed-demo-players:false}") boolean seedDemoPlayers) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedDemoPlayers = seedDemoPlayers;
    }

    @Override
    public void run(String... args) {
        if (!seedDemoPlayers || playerRepository.count() > 0) {
            return;
        }
        playerRepository.save(new Player("Angelo",  passwordEncoder.encode("angelo123")));
        playerRepository.save(new Player("Mouad",   passwordEncoder.encode("moad123")));
        playerRepository.save(new Player("Hunain",  passwordEncoder.encode("hunain123")));
        playerRepository.save(new Player("Lama",    passwordEncoder.encode("lama123")));
        playerRepository.save(new Player("Eddy",    passwordEncoder.encode("eddy123")));
        playerRepository.save(new Player("Tugberk", passwordEncoder.encode("tugberk123")));
        log.warn("Seeded six demo players with well-known passwords. Keep SEED_DEMO_PLAYERS off on a public deployment.");
    }
}
