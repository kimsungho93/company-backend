package com.ksh.companybackend.game.api.dto;

import com.ksh.companybackend.game.domain.Avatar;
import com.ksh.companybackend.game.domain.Player;

public record PlayerResponse(Long userId, String name, Avatar avatar, boolean ready) {

    public static PlayerResponse from(Player player) {
        return new PlayerResponse(player.userId(), player.name(), player.avatar(), player.ready());
    }
}
