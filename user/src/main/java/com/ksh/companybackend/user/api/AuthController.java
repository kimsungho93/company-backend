package com.ksh.companybackend.user.api;

import com.ksh.companybackend.user.api.dto.LoginRequest;
import com.ksh.companybackend.user.api.dto.LoginResponse;
import com.ksh.companybackend.user.api.dto.SignupRequest;
import com.ksh.companybackend.user.application.AuthService;
import com.ksh.companybackend.user.application.dto.LoginCommand;
import com.ksh.companybackend.user.application.dto.LoginResult;
import com.ksh.companybackend.user.application.dto.SignupCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public void signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(new SignupCommand(request.email(), request.name(), request.password()));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = authService.login(new LoginCommand(request.email(), request.password()));
        return new LoginResponse(result.accessToken(), result.expiresIn());
    }
}
