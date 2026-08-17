package com.ksh.companybackend.user.application;

import com.ksh.companybackend.user.application.dto.LoginCommand;
import com.ksh.companybackend.user.application.dto.LoginResult;
import com.ksh.companybackend.user.application.dto.SignupCommand;
import com.ksh.companybackend.user.domain.RefreshToken;
import com.ksh.companybackend.user.domain.RefreshTokenRepository;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import com.ksh.companybackend.user.domain.UserStatus;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final Duration refreshTokenTtl;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider,
            @Value("${app.refresh-token.ttl}") Duration refreshTokenTtl) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenTtl = refreshTokenTtl;
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

    @Transactional
    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.matchesPassword(command.rawPassword(), passwordEncoder)) {
            throw new InvalidCredentialsException();
        }

        verifyUsable(user);

        return issueTokensFor(user);
    }

    // noRollbackFor 를 빼면 안 된다. 재사용을 감지해 전부 폐기해놓고 예외를 던지는데,
    // 롤백되면 폐기가 통째로 사라져서 탈취자가 계속 쓸 수 있다.
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public LoginResult reissue(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new UnauthenticatedException();
        }

        RefreshToken used = refreshTokenRepository.findByTokenHash(RefreshToken.hash(rawRefreshToken))
                .orElseThrow(InvalidTokenException::new);

        if (used.isRevoked()) {
            revokeEveryTokenOf(used.getUserId());
            throw new InvalidTokenException();
        }

        if (used.isExpired()) {
            throw new TokenExpiredException();
        }

        User user = userRepository.findById(used.getUserId()).orElseThrow(InvalidTokenException::new);
        verifyUsable(user);

        used.revoke();

        return issueTokensFor(user);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        refreshTokenRepository.findByTokenHash(RefreshToken.hash(rawRefreshToken))
                .ifPresent(RefreshToken::revoke);
    }

    private void verifyUsable(User user) {
        if (user.getStatus() == UserStatus.REJECTED) {
            throw new SignupRejectedException();
        }
        if (user.getStatus() != UserStatus.APPROVED) {
            throw new ApprovalPendingException();
        }
    }

    private void revokeEveryTokenOf(Long userId) {
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId).forEach(RefreshToken::revoke);
    }

    private LoginResult issueTokensFor(User user) {
        RefreshToken.Issued issued = RefreshToken.issue(user.getId(), refreshTokenTtl);
        refreshTokenRepository.save(issued.token());

        return new LoginResult(
                tokenProvider.createAccessToken(user.getId(), user.getEmail()),
                tokenProvider.getAccessTokenTtlSeconds(),
                issued.rawValue());
    }
}
