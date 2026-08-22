package com.ksh.companybackend.calendar.domain;

import java.util.Collection;
import java.util.Map;

// calendar 가 사람에 대해 아는 것의 전부다. 쓰는 쪽이 자기 필요를 선언하고,
// 구현은 bootstrap 이 꽂는다 - user 모듈은 이 인터페이스의 존재를 모른다.
//
// 여기에 필드를 늘리기 전에 정말 표시용인지 볼 것. 부서·직급까지 필요해지면
// 계정이 아니라 사원 도메인이 필요한 것이다.
public interface UserDirectory {

    String nameOf(Long userId);

    Map<Long, String> namesOf(Collection<Long> userIds);
}
