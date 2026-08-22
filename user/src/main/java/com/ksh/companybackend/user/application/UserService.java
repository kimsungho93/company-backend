package com.ksh.companybackend.user.application;

import com.ksh.companybackend.user.application.dto.UserProfile;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfile findProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        return new UserProfile(user.getId(), user.getEmail(), user.getName(), user.getRole(), user.getStatus());
    }

    @Transactional(readOnly = true)
    public String nameOf(Long userId) {
        return userRepository.findById(userId).map(User::getName).orElse(null);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> namesOf(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return userRepository.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }
}
