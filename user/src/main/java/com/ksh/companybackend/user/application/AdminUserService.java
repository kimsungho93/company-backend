package com.ksh.companybackend.user.application;

import com.ksh.companybackend.user.application.dto.AdminUserSummary;
import com.ksh.companybackend.user.domain.RefreshToken;
import com.ksh.companybackend.user.domain.RefreshTokenRepository;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import com.ksh.companybackend.user.domain.UserStatus;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AdminUserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserSummary> findByStatus(Long callerId, UserStatus status) {
        verifyAdmin(callerId);

        return userRepository.findAllByStatusOrderByCreatedAtAsc(status).stream()
                .map(user -> new AdminUserSummary(
                        user.getId(), user.getEmail(), user.getName(), user.getStatus(), user.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void approve(Long callerId, Long targetId) {
        verifyAdmin(callerId);

        findTarget(targetId).approve();
    }

    @Transactional
    public void reject(Long callerId, Long targetId) {
        verifyAdmin(callerId);

        if (callerId.equals(targetId)) {
            throw new CannotRejectSelfException();
        }

        findTarget(targetId).reject();
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(targetId).forEach(RefreshToken::revoke);
    }

    // 역할을 토큰 클레임이 아니라 여기서 확인한다. 클레임에 담으면 강등해도
    // 액세스 토큰 수명 동안 관리자로 남는다.
    private void verifyAdmin(Long callerId) {
        User caller = userRepository.findById(callerId).orElseThrow(ForbiddenException::new);

        if (!caller.isAdmin()) {
            throw new ForbiddenException();
        }
    }

    private User findTarget(Long targetId) {
        return userRepository.findById(targetId).orElseThrow(UserNotFoundException::new);
    }
}
