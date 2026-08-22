package com.ksh.companybackend.game.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class RoomRegistry {

    private final Map<Long, Room> rooms = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public List<Room> findAllNewestFirst() {
        return rooms.values().stream()
                .sorted(Comparator.comparing(Room::id).reversed())
                .toList();
    }

    public Optional<Room> get(Long roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public Room find(Long roomId) {
        return get(roomId).orElseThrow(RoomNotFoundException::new);
    }

    public Room open(String name, String passwordHash, Player host) {
        Room room = Room.create(sequence.incrementAndGet(), name, passwordHash, host);
        rooms.put(room.id(), room);

        return room;
    }

    public Room join(Long roomId, Player player) {
        Room joined = rooms.computeIfPresent(roomId, (id, room) -> room.join(player));
        if (joined == null) {
            throw new RoomNotFoundException();
        }

        return joined;
    }

    public Room enter(Long roomId, Long userId, String sessionId) {
        Room entered = rooms.computeIfPresent(roomId, (id, room) -> room.enter(userId, sessionId));
        if (entered == null) {
            throw new RoomNotFoundException();
        }

        return entered;
    }

    // 사람이 방에서 빠지는 유일한 통로다. 빈 방 삭제가 여기 붙어 있어야
    // 트리거가 늘어도 규칙이 갈라지지 않는다.
    public Optional<Room> leave(Long roomId, Long userId) {
        return Optional.ofNullable(rooms.computeIfPresent(roomId, (id, room) -> {
            Room left = room.leave(userId);
            return left.isEmpty() ? null : left;
        }));
    }

    public void leaveOtherRooms(Long userId, Long keepRoomId) {
        rooms.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(keepRoomId))
                .filter(entry -> entry.getValue().has(userId))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(id -> leave(id, userId));
    }
}
