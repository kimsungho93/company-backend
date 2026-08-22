package com.ksh.companybackend.game.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
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

    public Room find(Long roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            throw new RoomNotFoundException();
        }

        return room;
    }

    public Room open(String name, String passwordHash, Long hostId) {
        Room room = Room.create(sequence.incrementAndGet(), name, passwordHash, hostId);
        rooms.put(room.id(), room);

        return room;
    }

    public Room join(Long roomId, Long userId) {
        Room joined = rooms.computeIfPresent(roomId, (id, room) -> room.join(userId));
        if (joined == null) {
            throw new RoomNotFoundException();
        }

        return joined;
    }

    public void leaveOtherRooms(Long userId, Long keepRoomId) {
        rooms.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(keepRoomId))
                .filter(entry -> entry.getValue().has(userId))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(id -> rooms.computeIfPresent(id, (k, room) -> {
                    Room left = room.leave(userId);
                    return left.isEmpty() ? null : left;
                }));
    }
}
