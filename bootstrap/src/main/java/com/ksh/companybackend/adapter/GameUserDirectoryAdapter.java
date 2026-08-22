package com.ksh.companybackend.adapter;

import com.ksh.companybackend.game.domain.UserDirectory;
import com.ksh.companybackend.user.application.UserService;
import java.util.Collection;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class GameUserDirectoryAdapter implements UserDirectory {

    private final UserService userService;

    public GameUserDirectoryAdapter(UserService userService) {
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
