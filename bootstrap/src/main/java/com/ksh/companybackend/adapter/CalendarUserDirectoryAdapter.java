package com.ksh.companybackend.adapter;

import com.ksh.companybackend.calendar.domain.UserDirectory;
import com.ksh.companybackend.user.application.UserService;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;

// 두 도메인 모듈을 잇는 자리라 조립 루트인 bootstrap 에 둔다.
// 어느 쪽 도메인에 두어도 그 모듈이 반대편을 알게 된다.
//
// user 의 리포지터리가 아니라 애플리케이션 API 에 위임한다. 조회와 트랜잭션은
// 데이터를 소유한 모듈이 갖는다.
@Service
public class CalendarUserDirectoryAdapter implements UserDirectory {

    private final UserService userService;

    public CalendarUserDirectoryAdapter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public String nameOf(Long userId) {
        return userService.nameOf(userId);
    }

    @Override
    public Map<Long, String> namesOf(Collection<Long> userIds) {
        return userService.namesOf(userIds);
    }
}
