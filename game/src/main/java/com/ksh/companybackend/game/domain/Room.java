package com.ksh.companybackend.game.domain;

import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class Room {

    private static final int CAPACITY = 10;
    private static final int MIN_PLAYERS = 2;

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

        return withPlayers(Stream.concat(players.stream(), Stream.of(player)).toList());
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
        return replace(userId, player -> player.withSession(sessionId));
    }

    public Room changeAvatar(Long userId, Avatar avatar) {
        return replace(userId, player -> player.withAvatar(avatar));
    }

    public Room changeReady(Long userId, boolean ready) {
        return replace(userId, player -> player.withReady(ready));
    }

    public Room transferTo(Long callerId, Long newHostId) {
        verifyHost(callerId);
        verifyPresent(newHostId);

        return new Room(id, name, passwordHash, newHostId, players, status);
    }

    public Room start(Long callerId) {
        verifyHost(callerId);
        if (players.size() < MIN_PLAYERS) {
            throw new NotEnoughPlayersException();
        }
        if (!everyoneButHostIsReady()) {
            throw new NotAllReadyException();
        }

        return new Room(id, name, passwordHash, hostId, players, RoomStatus.PLAYING);
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

    public List<Player> players() {
        return players;
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

    // 방장 자신의 준비 여부는 보지 않는다. 방장에게는 준비 버튼이 없고,
    // 값을 속이는 대신 시작 조건에서 빼기로 했다.
    private boolean everyoneButHostIsReady() {
        return players.stream()
                .filter(player -> !player.userId().equals(hostId))
                .allMatch(Player::ready);
    }

    private Room replace(Long userId, UnaryOperator<Player> change) {
        verifyPresent(userId);

        return withPlayers(players.stream()
                .map(player -> player.userId().equals(userId) ? change.apply(player) : player)
                .toList());
    }

    private Room withPlayers(List<Player> players) {
        return new Room(id, name, passwordHash, hostId, players, status);
    }

    private void verifyHost(Long userId) {
        if (!hostId.equals(userId)) {
            throw new NotRoomHostException();
        }
    }

    private void verifyPresent(Long userId) {
        if (!has(userId)) {
            throw new NotInRoomException();
        }
    }

    private static Long firstUserId(List<Player> players) {
        return players.stream().findFirst().map(Player::userId).orElse(null);
    }
}
