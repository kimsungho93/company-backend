package com.ksh.companybackend.game.application;

import com.ksh.companybackend.game.application.dto.RoomSummary;
import com.ksh.companybackend.game.domain.Avatar;
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
    private final RoomBroadcaster broadcaster;

    public RoomService(RoomRegistry roomRegistry, UserDirectory userDirectory,
            PasswordEncoder passwordEncoder, RoomBroadcaster broadcaster) {
        this.roomRegistry = roomRegistry;
        this.userDirectory = userDirectory;
        this.passwordEncoder = passwordEncoder;
        this.broadcaster = broadcaster;
    }

    public List<RoomSummary> findAll() {
        return roomRegistry.findAllNewestFirst().stream()
                .map(RoomSummary::of)
                .toList();
    }

    public boolean isParticipant(Long roomId, Long userId) {
        return roomRegistry.get(roomId).map(room -> room.has(userId)).orElse(false);
    }

    public RoomSummary open(Long hostId, String name, String password) {
        Room room = roomRegistry.open(name, hash(password), seat(hostId));
        roomRegistry.leaveOtherRooms(hostId, room.id());
        broadcaster.roomListChanged(findAll());

        return RoomSummary.of(room);
    }

    public RoomSummary join(Long userId, Long roomId, String password) {
        if (!roomRegistry.find(roomId).opensWith(password, passwordEncoder)) {
            throw new WrongRoomPasswordException();
        }
        Player player = seat(userId);
        roomRegistry.leaveOtherRooms(userId, roomId);

        Room joined = roomRegistry.join(roomId, player);
        broadcaster.roomChanged(joined);
        broadcaster.roomListChanged(findAll());

        return RoomSummary.of(joined);
    }

    public void enter(Long callerId, Long roomId, String sessionId) {
        broadcaster.roomChanged(roomRegistry.update(roomId, room -> room.enter(callerId, sessionId)));
    }

    public void changeAvatar(Long callerId, Long roomId, Avatar avatar) {
        broadcaster.roomChanged(roomRegistry.update(roomId, room -> room.changeAvatar(callerId, avatar)));
    }

    public void changeReady(Long callerId, Long roomId, boolean ready) {
        broadcaster.roomChanged(roomRegistry.update(roomId, room -> room.changeReady(callerId, ready)));
    }

    public void transfer(Long callerId, Long roomId, Long newHostId) {
        broadcaster.roomChanged(roomRegistry.update(roomId, room -> room.transferTo(callerId, newHostId)));
    }

    public void start(Long callerId, Long roomId) {
        broadcaster.roomChanged(roomRegistry.update(roomId, room -> room.start(callerId)));
        broadcaster.roomListChanged(findAll());
    }

    public void leave(Long callerId, Long roomId) {
        roomRegistry.leave(roomId, callerId).ifPresent(broadcaster::roomChanged);
        broadcaster.roomListChanged(findAll());
    }

    private Player seat(Long userId) {
        return Player.seat(userId, userDirectory.nameOf(userId));
    }

    private String hash(String password) {
        return password == null || password.isBlank() ? null : passwordEncoder.encode(password);
    }
}
