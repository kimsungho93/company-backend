package com.ksh.companybackend.game.domain;

public record Player(Long userId, String name, Avatar avatar, boolean ready, String sessionId) {

    public static Player seat(Long userId, String name) {
        return new Player(userId, name, null, false, null);
    }

    public Player withSession(String sessionId) {
        return new Player(userId, name, avatar, ready, sessionId);
    }

    public Player withAvatar(Avatar avatar) {
        return new Player(userId, name, avatar, ready, sessionId);
    }

    public Player withReady(boolean ready) {
        return new Player(userId, name, avatar, ready, sessionId);
    }
}
