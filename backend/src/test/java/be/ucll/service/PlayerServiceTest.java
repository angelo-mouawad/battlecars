package be.ucll.service;

import be.ucll.model.Player;
import be.ucll.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlayerServiceTest {

    @Mock private PlayerRepository repository;

    // Use a real BCryptPasswordEncoder — we want to test actual hash behaviour
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private PlayerService playerService;

    @BeforeEach
    void setUp() {
        playerService = new PlayerService(repository, passwordEncoder);
    }

    // =========================================================================
    // createPlayer
    // =========================================================================

    @Test
    void createPlayer_passwordIsHashed() {
        when(repository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        Player result = playerService.createPlayer("Lama", "lama123");

        // Password must not be stored as plain text
        assertThat(result.getPassword()).isNotEqualTo("lama123");
        // Must be a valid BCrypt hash
        assertThat(result.getPassword()).startsWith("$2a$");
    }

    @Test
    void createPlayer_nameIsSetCorrectly() {
        when(repository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        Player result = playerService.createPlayer("Mouad", "moad123");

        assertThat(result.getName()).isEqualTo("Mouad");
    }

    @Test
    void createPlayer_callsRepositorySave() {
        when(repository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        playerService.createPlayer("Test", "test123");

        verify(repository, times(1)).save(any(Player.class));
    }

    // =========================================================================
    // verifyPassword
    // =========================================================================

    @Test
    void verifyPassword_correctPassword_returnsTrue() {
        String hashed = passwordEncoder.encode("lama123");
        Player player = new Player("Lama", hashed);
        when(repository.findByName("Lama")).thenReturn(Optional.of(player));

        boolean result = playerService.verifyPassword("Lama", "lama123");

        assertThat(result).isTrue();
    }

    @Test
    void verifyPassword_wrongPassword_returnsFalse() {
        String hashed = passwordEncoder.encode("lama123");
        Player player = new Player("Lama", hashed);
        when(repository.findByName("Lama")).thenReturn(Optional.of(player));

        boolean result = playerService.verifyPassword("Lama", "wrongpassword");

        assertThat(result).isFalse();
    }

    @Test
    void verifyPassword_playerNotFound_throwsIllegalArgument() {
        when(repository.findByName("Unknown")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> playerService.verifyPassword("Unknown", "anypassword"));
    }

    @Test
    void verifyPassword_emptyPassword_returnsFalse() {
        String hashed = passwordEncoder.encode("lama123");
        Player player = new Player("Lama", hashed);
        when(repository.findByName("Lama")).thenReturn(Optional.of(player));

        boolean result = playerService.verifyPassword("Lama", "");

        assertThat(result).isFalse();
    }

    // =========================================================================
    // changePassword
    // =========================================================================

    @Test
    void changePassword_newPasswordIsHashed() {
        Player player = new Player("Lama", passwordEncoder.encode("oldpassword"));
        when(repository.findByName("Lama")).thenReturn(Optional.of(player));
        when(repository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        Player result = playerService.changePassword("Lama", "newpassword");

        assertThat(result.getPassword()).isNotEqualTo("newpassword");
        assertThat(result.getPassword()).startsWith("$2a$");
        // New password must verify correctly
        assertThat(passwordEncoder.matches("newpassword", result.getPassword())).isTrue();
    }

    @Test
    void changePassword_oldPasswordNoLongerValid() {
        Player player = new Player("Lama", passwordEncoder.encode("oldpassword"));
        when(repository.findByName("Lama")).thenReturn(Optional.of(player));
        when(repository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        Player result = playerService.changePassword("Lama", "newpassword");

        assertThat(passwordEncoder.matches("oldpassword", result.getPassword())).isFalse();
    }

    @Test
    void changePassword_playerNotFound_throwsIllegalArgument() {
        when(repository.findByName("Ghost")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> playerService.changePassword("Ghost", "newpassword"));
    }

    // =========================================================================
    // addPoints
    // =========================================================================

    @Test
    void addPoints_incrementsScoreCorrectly() {
        Player player = new Player("Lama", "hash");
        player.setScore(3);
        when(repository.findByName("Lama")).thenReturn(Optional.of(player));
        when(repository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        Player result = playerService.addPoints("Lama", 1);

        assertThat(result.getScore()).isEqualTo(4);
    }

    @Test
    void addPoints_playerNotFound_throwsIllegalArgument() {
        when(repository.findByName("Ghost")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> playerService.addPoints("Ghost", 1));
    }

    // =========================================================================
    // getAll
    // =========================================================================

    @Test
    void getAll_returnsAllPlayers() {
        Player p1 = new Player("Lama", "hash1");
        Player p2 = new Player("Mouad", "hash2");
        when(repository.findAll()).thenReturn(List.of(p1, p2));

        List<Player> result = playerService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Lama");
        assertThat(result.get(1).getName()).isEqualTo("Mouad");
    }

    @Test
    void getAll_emptyDatabase_returnsEmptyList() {
        when(repository.findAll()).thenReturn(List.of());

        List<Player> result = playerService.getAll();

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // deletePlayer
    // =========================================================================

    @Test
    void deletePlayer_existingPlayer_callsDelete() {
        Player player = new Player("Lama", "hash");
        ReflectionTestUtils.setField(player, "id", 1L);
        when(repository.findById(1L)).thenReturn(Optional.of(player));

        playerService.deletePlayer(1L);

        verify(repository, times(1)).delete(player);
    }

    @Test
    void deletePlayer_notFound_throwsIllegalArgument() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> playerService.deletePlayer(999L));
    }
}