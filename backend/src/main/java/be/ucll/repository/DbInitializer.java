package be.ucll.repository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import be.ucll.model.Player;

@Component
public class DbInitializer implements CommandLineRunner {

    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    public DbInitializer(PlayerRepository playerRepository, PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (playerRepository.count() == 0) {
            playerRepository.save(new Player("Angelo",  passwordEncoder.encode("angelo123")));
            playerRepository.save(new Player("Mouad",   passwordEncoder.encode("moad123")));
            playerRepository.save(new Player("Hunain",  passwordEncoder.encode("hunain123")));
            playerRepository.save(new Player("Lama",    passwordEncoder.encode("lama123")));
            playerRepository.save(new Player("Eddy",    passwordEncoder.encode("eddy123")));
            playerRepository.save(new Player("Tugberk", passwordEncoder.encode("tugberk123")));
        } else {
            // Re-hash any passwords that are still stored as plain text (no BCrypt prefix)
            for (Player player : playerRepository.findAll()) {
                String pw = player.getPassword();
                if (pw != null && !pw.startsWith("$2a$") && !pw.startsWith("$2b$") && !pw.startsWith("$2y$")) {
                    player.setPassword(passwordEncoder.encode(pw));
                    playerRepository.save(player);
                }
            }
        }
    }
}