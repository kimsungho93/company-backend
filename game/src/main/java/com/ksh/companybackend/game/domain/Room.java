package com.ksh.companybackend.game.domain;

import java.util.List;
import java.util.stream.Stream;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class Room {

    private static final int CAPACITY = 10;

    private final Long id;
    private final String name;
    private final String passwordHash;
    private final Long hostId;
    private final List<Player> players;
    private final RoomStatus status;

    private Room(Long id, String name, String passwordHash, Long hostId, List<Player> players, RoomStatus status) {
        this.id = id;
        this.name = name;
        this.passwordHash = passwordHash;
        this.hostId = hostId;
        this.players = players;
        this.status = status;
    }

    public static Room create(Long id, String name, String passwordHash, Player host) {
        return new Room(id, name, passwordHash, host.userId(), List.of(host), RoomStatus.WAITING);
    }

    public Room join(Player player) {
        if (has(player.userId())) {
            return this;
        }
        if (players.size() >= CAPACITY) {
            throw new RoomFullException();
        }

        return new Room(id, name, passwordHash, hostId,
                Stream.concat(players.stream(), Stream.of(player)).toList(), status);
    }

    public Room leave(Long userId) {
        if (!has(userId)) {
            return this;
        }

        List<Player> remaining = players.stream().filter(player -> !player.userId().equals(userId)).toList();
        Long nextHost = hostId.equals(userId) ? firstUserId(remaining) : hostId;

        return new Room(id, name, passwordHash, nextHost, remaining, status);
    }

    public Room enter(Long userId, String sessionId) {
        List<Player> entered = players.stream()
                .map(player -> player.userId().equals(userId) ? player.withSession(sessionId) : player)
                .toList();

        return new Room(id, name, passwordHash, hostId, entered, status);
    }

    public List<Player> players() {
        return players;
    }

    public boolean opensWith(String rawPassword, PasswordEncoder encoder) {
        return !isLocked() || (rawPassword != null && encoder.matches(rawPassword, passwordHash));
    }

    public boolean isLocked() {
        return passwordHash != null;
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public boolean has(Long userId) {
        return players.stream().anyMatch(player -> player.userId().equals(userId));
    }

    public int playerCount() {
        return players.size();
    }

    public int capacity() {
        return CAPACITY;
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Long hostId() {
        return hostId;
    }

    public String hostName() {
        return players.stream()
                .filter(player -> player.userId().equals(hostId))
                .findFirst()
                .map(Player::name)
                .orElse(null);
    }

    public RoomStatus status() {
        return status;
    }

    private static Long firstUserId(List<Player> players) {
        return players.stream().findFirst().map(Player::userId).orElse(null);
    }
}
