package com.ksh.companybackend.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Getter
    @Column(nullable = false, length = 50)
    private String name;

    @Getter
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private User(String email, String password, String name) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.createdAt = Instant.now();
    }

    public static User create(String email, String encodedPassword, String name) {
        return new User(email, encodedPassword, name);
    }

    public boolean matchesPassword(String rawPassword, PasswordEncoder encoder) {
        return encoder.matches(rawPassword, this.password);
    }
}
