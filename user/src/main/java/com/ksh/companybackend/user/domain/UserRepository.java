package com.ksh.companybackend.user.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface UserRepository extends Repository<User, Long> {

    Optional<User> findById(Long id);

    List<User> findAllByStatusOrderByCreatedAtAsc(UserStatus status);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void save(User user);
}
