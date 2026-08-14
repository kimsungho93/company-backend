package com.ksh.companybackend.user.application;

import com.ksh.companybackend.user.application.dto.LoginCommand;
import com.ksh.companybackend.user.application.dto.LoginResult;
import com.ksh.companybackend.user.application.dto.SignupCommand;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public void signup(SignupCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException();
        }

        String encodedPassword = passwordEncoder.encode(command.rawPassword());
        User user = User.create(command.email(), encodedPassword, command.name());

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.matchesPassword(command.rawPassword(), passwordEncoder)) {
            throw new InvalidCredentialsException();
        }

        return new LoginResult(
                tokenProvider.createAccessToken(user.getId(), user.getEmail()),
                tokenProvider.getAccessTokenTtlSeconds());
    }
}
