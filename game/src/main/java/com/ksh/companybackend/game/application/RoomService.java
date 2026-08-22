package com.ksh.companybackend.game.application;

import com.ksh.companybackend.game.application.dto.RoomSummary;
import com.ksh.companybackend.game.domain.Room;
import com.ksh.companybackend.game.domain.RoomRegistry;
import com.ksh.companybackend.game.domain.UserDirectory;
import com.ksh.companybackend.game.domain.WrongRoomPasswordException;
import java.util.List;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RoomService {

    private final RoomRegistry roomRegistry;
    private final UserDirectory userDirectory;
    private final PasswordEncoder passwordEncoder;

    public RoomService(RoomRegistry roomRegistry, UserDirectory userDirectory, PasswordEncoder passwordEncoder) {
        this.roomRegistry = roomRegistry;
        this.userDirectory = userDirectory;
        this.passwordEncoder = passwordEncoder;
    }

    public List<RoomSummary> findAll() {
        List<Room> rooms = roomRegistry.findAllNewestFirst();
        Map<Long, String> hostNames = userDirectory.namesOf(
                rooms.stream().map(Room::hostId).distinct().toList());

        return rooms.stream()
                .map(room -> RoomSummary.of(room, hostNames.get(room.hostId())))
                .toList();
    }

    public RoomSummary open(Long hostId, String name, String password) {
        Room room = roomRegistry.open(name, hash(password), hostId);
        roomRegistry.leaveOtherRooms(hostId, room.id());

        return summarize(room);
    }

    public RoomSummary join(Long userId, Long roomId, String password) {
        if (!roomRegistry.find(roomId).opensWith(password, passwordEncoder)) {
            throw new WrongRoomPasswordException();
        }
        roomRegistry.leaveOtherRooms(userId, roomId);

        return summarize(roomRegistry.join(roomId, userId));
    }

    private RoomSummary summarize(Room room) {
        return RoomSummary.of(room, userDirectory.nameOf(room.hostId()));
    }

    private String hash(String password) {
        return password == null || password.isBlank() ? null : passwordEncoder.encode(password);
    }
}
