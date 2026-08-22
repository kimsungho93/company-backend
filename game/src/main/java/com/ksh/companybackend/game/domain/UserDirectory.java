package com.ksh.companybackend.game.domain;

import java.util.Collection;
import java.util.Map;

// game 이 사람에 대해 아는 것의 전부다. 방장 이름을 그리는 용도뿐이라
// 여기에 권한이나 상태를 얹지 않는다 - 그건 다른 포트로 나뉘어야 한다.
public interface UserDirectory {

    String nameOf(Long userId);

    Map<Long, String> namesOf(Collection<Long> userIds);
}
