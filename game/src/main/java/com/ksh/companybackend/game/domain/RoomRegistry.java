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
        leaveOtherRooms(hostId, null);

        Room room = Room.create(sequence.incrementAndGet(), name, passwordHash, hostId);
        rooms.put(room.id(), room);

        return room;
    }

    // 정원 검사와 입장이 갈라지면 11명이 들어간다. compute 안에서 한 번에 끝낸다 -
    // 비밀번호 대조는 참가자와 무관하므로 이 구간 밖에서 미리 한다.
    public Room join(Long roomId, Long userId) {
        leaveOtherRooms(userId, roomId);

        Room joined = rooms.computeIfPresent(roomId, (id, room) -> room.join(userId));
        if (joined == null) {
            throw new RoomNotFoundException();
        }

        return joined;
    }

    private void leaveOtherRooms(Long userId, Long keep) {
        rooms.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(keep))
                .filter(entry -> entry.getValue().has(userId))
                .map(Map.Entry::getKey)
                .toList()
                .forEach(id -> rooms.computeIfPresent(id, (k, room) -> {
                    Room left = room.leave(userId);
                    return left.isEmpty() ? null : left;
                }));
    }
}
