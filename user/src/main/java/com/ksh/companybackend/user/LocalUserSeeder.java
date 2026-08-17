package com.ksh.companybackend.user;

import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("local")
public class LocalUserSeeder {

    private static final Logger log = LoggerFactory.getLogger(LocalUserSeeder.class);

    private static final String EMAIL = "test@ibslab.com";
    private static final String PASSWORD = "password1234";
    private static final String NAME = "테스트";

    @Bean
    ApplicationRunner seedLocalUser(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (users.findByEmail(EMAIL).isPresent()) {
                return;
            }
            User user = User.create(EMAIL, encoder.encode(PASSWORD), NAME);
            user.approve();
            users.save(user);

            log.info("로컬 테스트 계정 생성: {} / {}", EMAIL, PASSWORD);
        };
    }
}
