package be.ucll.model;

import jakarta.persistence.*;

@Entity
@Table(name = "match_players")
public class MatchPlayer {

    public static final int MAX_HEALTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    // Which physical car this player is driving (1 or 2)
    @Column(name = "car_id", nullable = false)
    private Integer carId;

    @Column(name = "health", nullable = false)
    private Integer health = MAX_HEALTH;

    @Column(name = "score")
    private Integer score;

    public MatchPlayer() {}

    public MatchPlayer(Match match, Player player, Integer carId) {
        this.match = match;
        this.player = player;
        this.carId = carId;
        this.health = MAX_HEALTH;
        this.score = 0;
    }

    public boolean isAlive() {
        return health > 0;
    }

    public void applyDamage(int damage) {
        this.health = Math.max(0, this.health - damage);
    }

    public Long getId() { return id; }

    public Match getMatch() { return match; }
    public void setMatch(Match match) { this.match = match; }

    public Player getPlayer() { return player; }
    public void setPlayer(Player player) { this.player = player; }

    public Integer getCarId() { return carId; }
    public void setCarId(Integer carId) { this.carId = carId; }

    public Integer getHealth() { return health; }
    public void setHealth(Integer health) { this.health = health; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}
