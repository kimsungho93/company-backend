package com.ksh.companybackend.game.application;

import com.ksh.companybackend.game.application.dto.RoomSummary;
import com.ksh.companybackend.game.domain.Room;
import java.util.List;

public interface RoomBroadcaster {

    void roomChanged(Room room);

    void roomListChanged(List<RoomSummary> rooms);
}
