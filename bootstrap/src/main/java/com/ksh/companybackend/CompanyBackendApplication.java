package com.ksh.companybackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CompanyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompanyBackendApplication.class, args);
    }

}
