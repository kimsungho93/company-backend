package com.ksh.companybackend.game.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoomTest {

    private static final Player HOST = Player.seat(1L, "김성호");

    private Room room() {
        return Room.create(7L, "점심내기 한판", null, HOST);
    }

    private Player player(long userId) {
        return Player.seat(userId, "참가자" + userId);
    }

    private Room withPlayers(int count) {
        Room room = room();
        for (long userId = 2; userId <= count; userId++) {
            room = room.join(player(userId));
        }

        return room;
    }

    @Test
    @DisplayName("방을 만들면 방장이 이미 들어가 있다")
    void hostJoinsOnCreate() {
        Room room = room();

        assertThat(room.playerCount()).isEqualTo(1);
        assertThat(room.hostId()).isEqualTo(HOST.userId());
        assertThat(room.has(HOST.userId())).isTrue();
        assertThat(room.status()).isEqualTo(RoomStatus.WAITING);
    }

    @Test
    @DisplayName("방장 이름을 방이 직접 안다 - 사용자 조회가 필요 없다")
    void knowsItsHostName() {
        assertThat(room().hostName()).isEqualTo("김성호");
    }

    @Test
    @DisplayName("정원 10명까지 들어가고 11번째는 거부한다")
    void rejectsEleventhPlayer() {
        Room full = withPlayers(10);

        assertThat(full.playerCount()).isEqualTo(10);
        assertThatThrownBy(() -> full.join(player(11L))).isInstanceOf(RoomFullException.class);
    }

    @Test
    @DisplayName("이미 들어온 사람이 또 들어와도 인원이 늘지 않는다")
    void joiningTwiceDoesNotCount() {
        Room room = room().join(player(2L)).join(player(2L));

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
        assertThat(room().leave(HOST.userId()).isEmpty()).isTrue();
    }

    @Test
    @DisplayName("방장이 나가면 먼저 들어온 사람에게 넘어가고 이름도 따라간다")
    void handsHostOverInJoinOrder() {
        Room room = room().join(player(2L)).join(player(3L));

        Room afterHostLeft = room.leave(HOST.userId());

        assertThat(afterHostLeft.hostId()).isEqualTo(2L);
        assertThat(afterHostLeft.hostName()).isEqualTo("참가자2");
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

    @Test
    @DisplayName("enter 하면 그 사람에게만 세션이 붙는다")
    void bindsSessionOnEnter() {
        Room room = room().join(player(2L)).enter(2L, "sess-2");

        assertThat(room.players()).extracting(Player::userId, Player::sessionId)
                .containsExactly(tuple(1L, null), tuple(2L, "sess-2"));
    }

    private Room readyRoom() {
        return room().join(player(2L)).join(player(3L)).changeReady(2L, true).changeReady(3L, true);
    }

    @Test
    @DisplayName("아바타를 고르면 그 사람만 바뀐다")
    void changesOnlyOwnAvatar() {
        Room room = room().join(player(2L)).changeAvatar(2L, Avatar.FEMALE);

        assertThat(room.players()).extracting(Player::userId, Player::avatar)
                .containsExactly(tuple(1L, null), tuple(2L, Avatar.FEMALE));
    }

    @Test
    @DisplayName("준비는 토글이 아니라 값을 받는다")
    void setsReadyByValue() {
        Room room = room().join(player(2L));

        assertThat(room.changeReady(2L, true).changeReady(2L, true).players().get(1).ready()).isTrue();
        assertThat(room.changeReady(2L, true).changeReady(2L, false).players().get(1).ready()).isFalse();
    }

    @Test
    @DisplayName("방에 없는 사람은 아바타도 준비도 바꾸지 못한다")
    void rejectsChangesFromOutsider() {
        assertThatThrownBy(() -> room().changeAvatar(99L, Avatar.MALE)).isInstanceOf(NotInRoomException.class);
        assertThatThrownBy(() -> room().changeReady(99L, true)).isInstanceOf(NotInRoomException.class);
    }

    @Test
    @DisplayName("방장만 양도할 수 있고 방에 있는 사람에게만 넘긴다")
    void transfersHostOnlyByHost() {
        Room room = room().join(player(2L));

        assertThat(room.transferTo(1L, 2L).hostId()).isEqualTo(2L);
        assertThatThrownBy(() -> room.transferTo(2L, 1L)).isInstanceOf(NotRoomHostException.class);
        assertThatThrownBy(() -> room.transferTo(1L, 99L)).isInstanceOf(NotInRoomException.class);
    }

    @Test
    @DisplayName("양도해도 그 사람의 준비 상태는 그대로다")
    void keepsReadyThroughTransfer() {
        Room room = room().join(player(2L)).changeReady(2L, true).transferTo(1L, 2L);

        assertThat(room.players().get(1).ready()).isTrue();
    }

    @Test
    @DisplayName("방장만 시작할 수 있다")
    void startsOnlyByHost() {
        assertThatThrownBy(() -> readyRoom().start(2L)).isInstanceOf(NotRoomHostException.class);
    }

    @Test
    @DisplayName("혼자서는 시작할 수 없다")
    void rejectsStartAlone() {
        assertThatThrownBy(() -> room().start(1L)).isInstanceOf(NotEnoughPlayersException.class);
    }

    @Test
    @DisplayName("준비하지 않은 사람이 있으면 시작할 수 없다")
    void rejectsStartBeforeEveryoneIsReady() {
        Room room = room().join(player(2L)).join(player(3L)).changeReady(2L, true);

        assertThatThrownBy(() -> room.start(1L)).isInstanceOf(NotAllReadyException.class);
    }

    @Test
    @DisplayName("방장을 뺀 전원이 준비하면 시작된다 - 방장 자신의 준비 여부는 보지 않는다")
    void startsWhenEveryoneButHostIsReady() {
        assertThat(readyRoom().start(1L).status()).isEqualTo(RoomStatus.PLAYING);
    }
}
