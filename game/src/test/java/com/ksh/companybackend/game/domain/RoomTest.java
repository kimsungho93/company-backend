package com.ksh.companybackend.game.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoomTest {

    private static final Long HOST = 1L;

    private Room room() {
        return Room.create(7L, "점심내기 한판", null, HOST);
    }

    private Room withPlayers(int count) {
        Room room = room();
        for (long id = 2; id <= count; id++) {
            room = room.join(id);
        }

        return room;
    }

    @Test
    @DisplayName("방을 만들면 방장이 이미 들어가 있다")
    void hostJoinsOnCreate() {
        Room room = room();

        assertThat(room.playerCount()).isEqualTo(1);
        assertThat(room.hostId()).isEqualTo(HOST);
        assertThat(room.has(HOST)).isTrue();
        assertThat(room.status()).isEqualTo(RoomStatus.WAITING);
    }

    @Test
    @DisplayName("정원 10명까지 들어가고 11번째는 거부한다")
    void rejectsEleventhPlayer() {
        Room full = withPlayers(10);

        assertThat(full.playerCount()).isEqualTo(10);
        assertThatThrownBy(() -> full.join(11L)).isInstanceOf(RoomFullException.class);
    }

    @Test
    @DisplayName("이미 들어온 사람이 또 들어와도 인원이 늘지 않는다")
    void joiningTwiceDoesNotCount() {
        Room room = room().join(2L).join(2L);

        assertThat(room.playerCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("정원이 찼어도 이미 있는 사람은 다시 들어올 수 있다")
    void alreadyInsideCanRejoinFullRoom() {
        Room full = withPlayers(10);

        assertThat(full.join(HOST).playerCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("마지막 사람이 나가면 빈 방이 된다")
    void becomesEmptyWhenLastLeaves() {
        assertThat(room().leave(HOST).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("방장이 나가면 먼저 들어온 순으로 방장이 넘어간다")
    void handsHostOverInJoinOrder() {
        Room room = room().join(2L).join(3L);

        assertThat(room.leave(HOST).hostId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("없는 사람이 나가도 아무 일이 없다")
    void leavingWhenNotInsideChangesNothing() {
        assertThat(room().leave(99L).playerCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("비밀번호가 있으면 잠긴 방이다")
    void locksWhenPasswordIsSet() {
        assertThat(room().isLocked()).isFalse();
        assertThat(Room.create(7L, "비밀방", "hashed", HOST).isLocked()).isTrue();
    }
}
