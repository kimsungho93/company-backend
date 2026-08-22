package com.ksh.companybackend.adapter;

import com.ksh.companybackend.calendar.domain.UserRoles;
import com.ksh.companybackend.user.application.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserRolesAdapter implements UserRoles {

    private final UserService userService;

    public UserRolesAdapter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean isAdmin(Long userId) {
        return userService.isAdmin(userId);
    }
}
