package com.ksh.companybackend.game.domain;

import java.time.Instant;

public record Player(Long userId, String name, Avatar avatar, boolean ready, String sessionId, Instant seatedAt) {

    public static Player seat(Long userId, String name, Instant seatedAt) {
        return new Player(userId, name, null, false, null, seatedAt);
    }

    public Player withSession(String sessionId) {
        return new Player(userId, name, avatar, ready, sessionId, seatedAt);
    }

    public Player withAvatar(Avatar avatar) {
        return new Player(userId, name, avatar, ready, sessionId, seatedAt);
    }

    public Player withReady(boolean ready) {
        return new Player(userId, name, avatar, ready, sessionId, seatedAt);
    }
}
