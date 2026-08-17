package com.ksh.companybackend.user.api;

import com.ksh.companybackend.user.api.dto.MeResponse;
import com.ksh.companybackend.user.application.UserService;
import com.ksh.companybackend.user.application.dto.UserProfile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Long callerId) {
        UserProfile profile = userService.findProfile(callerId);

        return new MeResponse(profile.id(), profile.email(), profile.name(), profile.role(), profile.status());
    }
}
