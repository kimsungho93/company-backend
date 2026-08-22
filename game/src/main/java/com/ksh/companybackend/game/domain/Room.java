package com.ksh.companybackend.game.domain;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class Room {

    private static final int CAPACITY = 10;

    private final Long id;
    private final String name;
    private final String passwordHash;
    private final Long hostId;
    private final Set<Long> players;
    private final RoomStatus status;

    private Room(Long id, String name, String passwordHash, Long hostId, Set<Long> players, RoomStatus status) {
        this.id = id;
        this.name = name;
        this.passwordHash = passwordHash;
        this.hostId = hostId;
        this.players = players;
        this.status = status;
    }

    public static Room create(Long id, String name, String passwordHash, Long hostId) {
        Set<Long> players = new LinkedHashSet<>();
        players.add(hostId);

        return new Room(id, name, passwordHash, hostId, players, RoomStatus.WAITING);
    }

    public Room join(Long userId) {
        if (players.contains(userId)) {
            return this;
        }
        if (players.size() >= CAPACITY) {
            throw new RoomFullException();
        }

        Set<Long> joined = new LinkedHashSet<>(players);
        joined.add(userId);

        return new Room(id, name, passwordHash, hostId, joined, status);
    }

    public Room leave(Long userId) {
        if (!players.contains(userId)) {
            return this;
        }

        Set<Long> remaining = new LinkedHashSet<>(players);
        remaining.remove(userId);
        Long nextHost = hostId.equals(userId) ? remaining.stream().findFirst().orElse(null) : hostId;

        return new Room(id, name, passwordHash, nextHost, remaining, status);
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
        return players.contains(userId);
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

    public RoomStatus status() {
        return status;
    }
}
