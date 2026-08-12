package com.ksh.companybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

/**
 * UserDetailsServiceAutoConfiguration 을 뺀 이유:
 * 이게 살아 있으면 기동할 때마다 임시 계정을 만들고 비밀번호를 로그에 찍는다.
 * 폼 로그인을 껐으니 쓸 수 없는 계정인데 로그만 헷갈리게 한다.
 * 실제 UserDetailsService 를 만들면 이 exclude 는 지워도 된다.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CompanyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompanyBackendApplication.class, args);
    }

}
