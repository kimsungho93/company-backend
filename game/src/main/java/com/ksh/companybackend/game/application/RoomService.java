package com.ksh.companybackend.game.application;

import com.ksh.companybackend.game.application.dto.RoomSummary;
import com.ksh.companybackend.game.domain.Player;
import com.ksh.companybackend.game.domain.Room;
import com.ksh.companybackend.game.domain.RoomRegistry;
import com.ksh.companybackend.game.domain.UserDirectory;
import com.ksh.companybackend.game.domain.WrongRoomPasswordException;
import java.util.List;
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
        return roomRegistry.findAllNewestFirst().stream()
                .map(RoomSummary::of)
                .toList();
    }

    public RoomSummary open(Long hostId, String name, String password) {
        Room room = roomRegistry.open(name, hash(password), seat(hostId));
        roomRegistry.leaveOtherRooms(hostId, room.id());

        return RoomSummary.of(room);
    }

    public RoomSummary join(Long userId, Long roomId, String password) {
        if (!roomRegistry.find(roomId).opensWith(password, passwordEncoder)) {
            throw new WrongRoomPasswordException();
        }
        Player player = seat(userId);
        roomRegistry.leaveOtherRooms(userId, roomId);

        return RoomSummary.of(roomRegistry.join(roomId, player));
    }

    private Player seat(Long userId) {
        return new Player(userId, userDirectory.nameOf(userId));
    }

    private String hash(String password) {
        return password == null || password.isBlank() ? null : passwordEncoder.encode(password);
    }
}
