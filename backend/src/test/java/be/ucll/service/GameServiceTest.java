package be.ucll.service;

import be.ucll.model.Match;
import be.ucll.model.Match.Status;
import be.ucll.model.MatchPlayer;
import be.ucll.model.Player;
import be.ucll.repository.MatchPlayerRepository;
import be.ucll.repository.MatchRepository;
import be.ucll.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GameServiceTest {

    @Mock private MatchRepository matchRepository;
    @Mock private MatchPlayerRepository matchPlayerRepository;
    @Mock private PlayerRepository playerRepository;

    @InjectMocks
    private GameService gameService;

    // Shared test data
    private Player player1;
    private Player player2;
    private Match activeMatch;
    private MatchPlayer mp1;
    private MatchPlayer mp2;

    @BeforeEach
    void setUp() {
        player1 = new Player("Lama", "hashed");
        ReflectionTestUtils.setField(player1, "id", 1L);
        player1.setScore(0);

        player2 = new Player("Mouad", "hashed");
        ReflectionTestUtils.setField(player2, "id", 2L);
        player2.setScore(0);

        activeMatch = new Match(Status.ACTIVE);
        ReflectionTestUtils.setField(activeMatch, "id", 10L);

        mp1 = new MatchPlayer(activeMatch, player1, 1);
        mp2 = new MatchPlayer(activeMatch, player2, 2);
    }

    // =========================================================================
    // createMatch
    // =========================================================================

    @Test
    void createMatch_validPlayers_returnsMatchIdAndPlayerInfo() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(player2));
        when(matchRepository.findFirstByStatus(Status.ACTIVE)).thenReturn(Optional.empty());
        when(matchRepository.findFirstByStatus(Status.WAITING)).thenReturn(Optional.empty());

        Match savedMatch = new Match(Status.WAITING);
        ReflectionTestUtils.setField(savedMatch, "id", 99L);
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = gameService.createMatch(1L, 1, 2L, 2);

        assertThat(result.get("matchId")).isEqualTo(99L);
        assertThat(result.get("status")).isEqualTo("WAITING");
        assertThat(result.get("player1")).isNotNull();
        assertThat(result.get("player2")).isNotNull();
    }

    @Test
    void createMatch_samePlayer_throwsIllegalArgument() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));

        assertThrows(IllegalArgumentException.class,
                () -> gameService.createMatch(1L, 1, 1L, 2));
    }

    @Test
    void createMatch_sameCar_throwsIllegalArgument() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(player2));

        assertThrows(IllegalArgumentException.class,
                () -> gameService.createMatch(1L, 1, 2L, 1));
    }

    @Test
    void createMatch_playerNotFound_throwsIllegalArgument() {
        when(playerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> gameService.createMatch(99L, 1, 2L, 2));
    }

    @Test
    void createMatch_activeMatchExists_closesItAndCreatesNew() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(player2));

        Match runningMatch = new Match(Status.ACTIVE);
        ReflectionTestUtils.setField(runningMatch, "id", 5L);
        when(matchRepository.findFirstByStatus(Status.ACTIVE)).thenReturn(Optional.of(runningMatch));
        when(matchRepository.findFirstByStatus(Status.WAITING)).thenReturn(Optional.empty());
        when(matchRepository.save(any(Match.class))).thenAnswer(i -> {
            Match m = i.getArgument(0);
            if (m.getId() == null) ReflectionTestUtils.setField(m, "id", 99L);
            return m;
        });
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = gameService.createMatch(1L, 1, 2L, 2);

        // Old match should be closed
        assertThat(runningMatch.getStatus()).isEqualTo(Status.FINISHED);
        // New match should be created
        assertThat(result.get("matchId")).isNotNull();
    }

    @Test
    void createMatch_waitingMatchExists_closesItAndCreatesNew() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(player2));

        Match waitingMatch = new Match(Status.WAITING);
        ReflectionTestUtils.setField(waitingMatch, "id", 5L);
        when(matchRepository.findFirstByStatus(Status.ACTIVE)).thenReturn(Optional.empty());
        when(matchRepository.findFirstByStatus(Status.WAITING)).thenReturn(Optional.of(waitingMatch));
        when(matchRepository.save(any(Match.class))).thenAnswer(i -> {
            Match m = i.getArgument(0);
            if (m.getId() == null) ReflectionTestUtils.setField(m, "id", 99L);
            return m;
        });
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = gameService.createMatch(1L, 1, 2L, 2);

        assertThat(waitingMatch.getStatus()).isEqualTo(Status.FINISHED);
        assertThat(result.get("matchId")).isNotNull();
    }

    @Test
    void createMatch_savesTwoMatchPlayers() {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player1));
        when(playerRepository.findById(2L)).thenReturn(Optional.of(player2));
        when(matchRepository.findFirstByStatus(any())).thenReturn(Optional.empty());

        Match savedMatch = new Match(Status.WAITING);
        ReflectionTestUtils.setField(savedMatch, "id", 99L);
        when(matchRepository.save(any(Match.class))).thenReturn(savedMatch);
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));

        gameService.createMatch(1L, 1, 2L, 2);

        // Exactly 2 MatchPlayer rows must be saved
        verify(matchPlayerRepository, times(2)).save(any(MatchPlayer.class));
    }

    // =========================================================================
    // startMatch
    // =========================================================================

    @Test
    void startMatch_waitingMatch_movesToActive() {
        Match waitingMatch = new Match(Status.WAITING);
        ReflectionTestUtils.setField(waitingMatch, "id", 10L);
        when(matchRepository.findById(10L)).thenReturn(Optional.of(waitingMatch));
        when(matchRepository.save(any(Match.class))).thenReturn(waitingMatch);
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));

        gameService.startMatch(10L);

        assertThat(waitingMatch.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(waitingMatch.getStartedAt()).isNotNull();
    }

    @Test
    void startMatch_alreadyActiveMatch_throwsIllegalState() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));

        assertThrows(IllegalStateException.class,
                () -> gameService.startMatch(10L));
    }

    @Test
    void startMatch_matchNotFound_throwsIllegalArgument() {
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> gameService.startMatch(999L));
    }

    // =========================================================================
    // processHit, damage logic
    // =========================================================================

    @Test
    void processHit_defaultDamage_subtractsFromBothPlayers() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));

        gameService.processHit(10L, 1, 10);

        assertThat(mp1.getHealth()).isEqualTo(40); // car 1 hit, loses 10
        assertThat(mp2.getHealth()).isEqualTo(50); // car 2 NOT hit, unchanged
    }

    @Test
    void processHit_lightDamage_subtracts5FromBoth() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));

        gameService.processHit(10L, 1, 5);

        assertThat(mp1.getHealth()).isEqualTo(45); // car 1 hit
        assertThat(mp2.getHealth()).isEqualTo(50); // car 2 unchanged
    }

    @Test
    void processHit_hardDamage_subtracts15FromBoth() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));

        gameService.processHit(10L, 1, 15);

        assertThat(mp1.getHealth()).isEqualTo(35); // car 1 hit
        assertThat(mp2.getHealth()).isEqualTo(50); // car 2 unchanged
    }

    @Test
    void processHit_returnsCorrectHealthInResult() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = gameService.processHit(10L, 1, 10);

        assertThat(result.get("car1Health")).isEqualTo(40); // car 1 hit
        assertThat(result.get("car2Health")).isEqualTo(50); // car 2 unchanged
        assertThat(result.get("matchOver")).isEqualTo(false);
        assertThat(result.get("type")).isEqualTo("HIT_RESULT");
    }

    @Test
    void processHit_healthCannotGoBelowZero() {
        mp1.setHealth(5);
        // mp2 stays at 50, only car 1 gets hit

        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));
        when(matchRepository.save(any(Match.class))).thenReturn(activeMatch);
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        gameService.processHit(10L, 1, 10);

        assertThat(mp1.getHealth()).isEqualTo(0); // clamped to 0
        assertThat(mp2.getHealth()).isEqualTo(50); // unchanged
    }

    // =========================================================================
    // processHit, match ending logic
    // =========================================================================

    @Test
    void processHit_onePlayerDies_matchIsFinished() {
        // Give mp1 exactly 10 HP so one hit kills them
        mp1.setHealth(10);
        mp2.setHealth(50);

        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));
        when(matchRepository.save(any(Match.class))).thenReturn(activeMatch);
        when(playerRepository.save(any(Player.class))).thenReturn(player2);

        Map<String, Object> result = gameService.processHit(10L, 1, 10);

        assertThat(result.get("matchOver")).isEqualTo(true);
        assertThat(activeMatch.getStatus()).isEqualTo(Status.FINISHED);
        assertThat(activeMatch.getEndedAt()).isNotNull();
    }

    @Test
    void processHit_onePlayerDies_correctWinnerReturned() {
        mp1.setHealth(10);
        mp2.setHealth(50);

        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));
        when(matchRepository.save(any(Match.class))).thenReturn(activeMatch);
        when(playerRepository.save(any(Player.class))).thenReturn(player2);

        Map<String, Object> result = gameService.processHit(10L, 1, 10);

        // mp2 (player2 / Mouad) should be the winner, they survived
        assertThat(result.get("winnerName")).isEqualTo("Mouad");
        assertThat(result.get("winnerCarId")).isEqualTo(2);
    }

    @Test
    void processHit_winnerGetsPointAdded() {
        mp1.setHealth(10);
        mp2.setHealth(50);

        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));
        when(matchRepository.save(any(Match.class))).thenReturn(activeMatch);

        ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);
        when(playerRepository.save(playerCaptor.capture())).thenReturn(player2);

        gameService.processHit(10L, 1, 10);

        // Winner's score should have been incremented by 1
        Player savedWinner = playerCaptor.getValue();
        assertThat(savedWinner.getScore()).isEqualTo(1);
    }

    @Test
    void processHit_hitCarDies_otherCarWins() {
        mp1.setHealth(10);
        // mp2 stays at 50

        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));
        when(matchPlayerRepository.save(any(MatchPlayer.class))).thenAnswer(i -> i.getArgument(0));
        when(matchRepository.save(any(Match.class))).thenReturn(activeMatch);
        when(playerRepository.save(any(Player.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> result = gameService.processHit(10L, 1, 10);

        assertThat(result.get("matchOver")).isEqualTo(true);
        assertThat(result.containsKey("draw")).isFalse();
        assertThat(result.get("winnerId")).isNotNull();
    }

    @Test
    void processHit_matchNotActive_throwsIllegalState() {
        Match finishedMatch = new Match(Status.FINISHED);
        ReflectionTestUtils.setField(finishedMatch, "id", 10L);
        when(matchRepository.findById(10L)).thenReturn(Optional.of(finishedMatch));

        assertThrows(IllegalStateException.class,
                () -> gameService.processHit(10L, 1, 10));
    }

    @Test
    void processHit_matchNotFound_throwsIllegalArgument() {
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> gameService.processHit(999L, 1, 10));
    }

    // =========================================================================
    // endMatch (manual force-end)
    // =========================================================================

    @Test
    void endMatch_validWinner_finishesMatch() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchIdAndPlayerId(10L, 1L)).thenReturn(Optional.of(mp1));
        when(matchRepository.save(any(Match.class))).thenReturn(activeMatch);
        when(playerRepository.save(any(Player.class))).thenReturn(player1);
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));

        gameService.endMatch(10L, 1L);

        assertThat(activeMatch.getStatus()).isEqualTo(Status.FINISHED);
        assertThat(activeMatch.getWinner()).isEqualTo(player1);
    }

    @Test
    void endMatch_winnerNotInMatch_throwsIllegalState() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchIdAndPlayerId(10L, 99L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class,
                () -> gameService.endMatch(10L, 99L));
    }

    // =========================================================================
    // getMatchState
    // =========================================================================

    @Test
    void getMatchState_returnsStatusAndPlayers() {
        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));

        Map<String, Object> state = gameService.getMatchState(10L);

        assertThat(state.get("matchId")).isEqualTo(10L);
        assertThat(state.get("status")).isEqualTo("ACTIVE");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> players = (List<Map<String, Object>>) state.get("players");
        assertThat(players).hasSize(2);
    }

    @Test
    void getMatchState_matchNotFound_throwsIllegalArgument() {
        when(matchRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> gameService.getMatchState(999L));
    }

    @Test
    void getMatchState_finishedMatchIncludesWinner() {
        Match finishedMatch = new Match(Status.FINISHED);
        ReflectionTestUtils.setField(finishedMatch, "id", 10L);
        finishedMatch.setWinner(player1);
        when(matchRepository.findById(10L)).thenReturn(Optional.of(finishedMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));

        Map<String, Object> state = gameService.getMatchState(10L);

        assertThat(state.get("winnerId")).isEqualTo(1L);
        assertThat(state.get("winnerName")).isEqualTo("Lama");
    }

    // =========================================================================
    // getActiveMatch
    // =========================================================================

    @Test
    void getActiveMatch_noActiveMatch_returnsNull() {
        when(matchRepository.findFirstByStatus(Status.ACTIVE)).thenReturn(Optional.empty());

        Map<String, Object> result = gameService.getActiveMatch();

        assertThat(result).isNull();
    }

    @Test
    void getActiveMatch_activeMatchExists_returnsState() {
        when(matchRepository.findFirstByStatus(Status.ACTIVE)).thenReturn(Optional.of(activeMatch));
        when(matchRepository.findById(10L)).thenReturn(Optional.of(activeMatch));
        when(matchPlayerRepository.findByMatchId(10L)).thenReturn(List.of(mp1, mp2));

        Map<String, Object> result = gameService.getActiveMatch();

        assertThat(result).isNotNull();
        assertThat(result.get("matchId")).isEqualTo(10L);
    }
}