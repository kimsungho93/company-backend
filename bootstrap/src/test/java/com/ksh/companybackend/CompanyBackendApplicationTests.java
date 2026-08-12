package com.ksh.companybackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CompanyBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
