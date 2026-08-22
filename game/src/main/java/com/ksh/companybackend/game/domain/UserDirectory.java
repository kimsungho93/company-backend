package com.ksh.companybackend.game.domain;

// game 이 사람에 대해 아는 것의 전부다. 좌석을 만들 때 이름을 한 번 읽고,
// 그 뒤로는 Player 가 들고 있어 다시 묻지 않는다.
public interface UserDirectory {

    String nameOf(Long userId);
}
