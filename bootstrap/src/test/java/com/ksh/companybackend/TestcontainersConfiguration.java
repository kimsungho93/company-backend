package com.ksh.companybackend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 테스트용 MySQL 컨테이너.
 *
 * <p>@ServiceConnection 이 컨테이너의 접속 정보(url/username/password)를 datasource 에 자동으로 꽂아준다.
 * 그래서 application.yaml 에 접속 정보를 적지 않는다.
 *
 * <p>컨테이너는 테스트 시작할 때 뜨고 끝나면 사라진다. 로컬 company-db 가 꺼져 있어도 테스트는 돈다.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer("mysql:8.4");
    }
}
