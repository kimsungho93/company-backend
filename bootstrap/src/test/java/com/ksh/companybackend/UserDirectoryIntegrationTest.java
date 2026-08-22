package com.ksh.companybackend;

import static org.assertj.core.api.Assertions.assertThat;

import com.ksh.companybackend.calendar.domain.UserDirectory;
import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class UserDirectoryIntegrationTest {

    @Autowired
    private UserDirectory userDirectory;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User save(String email, String name) {
        User user = User.create(email, passwordEncoder.encode("password1234"), name);
        users.save(user);
        return user;
    }

    @Test
    @DisplayName("id 로 이름을 찾는다")
    void findsNamesByIds() {
        User park = save("park@ibslab.com", "박철수");
        User kim = save("kim@ibslab.com", "김영희");

        Map<Long, String> names = userDirectory.namesOf(List.of(park.getId(), kim.getId()));

        assertThat(names).containsOnly(
                Map.entry(park.getId(), "박철수"),
                Map.entry(kim.getId(), "김영희"));
    }

    @Test
    @DisplayName("없는 id 는 결과에서 빠진다 - 부르는 쪽이 null 을 받지 않는다")
    void skipsUnknownIds() {
        User park = save("park@ibslab.com", "박철수");

        Map<Long, String> names = userDirectory.namesOf(List.of(park.getId(), 9_999_999L));

        assertThat(names).containsOnlyKeys(park.getId());
    }

    @Test
    @DisplayName("한 사람은 단건으로 찾는다")
    void findsSingleName() {
        User park = save("park@ibslab.com", "박철수");

        assertThat(userDirectory.nameOf(park.getId())).isEqualTo("박철수");
    }

    @Test
    @DisplayName("없는 사람의 단건 조회는 null")
    void singleNameOfUnknownIsNull() {
        assertThat(userDirectory.nameOf(9_999_999L)).isNull();
    }

    @Test
    @DisplayName("빈 목록이면 빈 결과")
    void handlesEmptyInput() {
        assertThat(userDirectory.namesOf(List.of())).isEmpty();
    }
}
