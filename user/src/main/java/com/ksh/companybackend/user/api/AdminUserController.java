package com.ksh.companybackend.user.api;

import com.ksh.companybackend.user.api.dto.AdminUserResponse;
import com.ksh.companybackend.user.application.AdminUserService;
import com.ksh.companybackend.user.domain.UserStatus;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public List<AdminUserResponse> findByStatus(@AuthenticationPrincipal Long callerId,
            @RequestParam UserStatus status) {
        return adminUserService.findByStatus(callerId, status).stream()
                .map(summary -> new AdminUserResponse(
                        summary.id(), summary.email(), summary.name(), summary.status(), summary.createdAt()))
                .toList();
    }

    @PostMapping("/{id}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(@AuthenticationPrincipal Long callerId, @PathVariable Long id) {
        adminUserService.approve(callerId, id);
    }

    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(@AuthenticationPrincipal Long callerId, @PathVariable Long id) {
        adminUserService.reject(callerId, id);
    }
}
