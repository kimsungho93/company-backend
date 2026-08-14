package com.ksh.companybackend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ksh.companybackend.user.domain.User;
import com.ksh.companybackend.user.domain.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class AuthSignupIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String body(String email, String name, String password) {
        return """
                {"email":%s,"name":%s,"password":%s}
                """.formatted(json(email), json(name), json(password));
    }

    private String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }

    private void perform201(String email, String name, String password) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, name, password)))
                .andExpect(status().isCreated());
    }

    private void perform400(String email, String name, String password, String expectedMessage) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(email, name, password)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    @Test
    @DisplayName("가입에 성공하면 201 과 빈 본문, 비밀번호는 해시로 저장된다")
    void signupSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("tiger@ibslab.com", "김성호", "password1234")))
                .andExpect(status().isCreated())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEmpty());

        User saved = users.findByEmail("tiger@ibslab.com").orElseThrow();
        assertThat(saved.getName()).isEqualTo("김성호");
        assertThat(saved.matchesPassword("password1234", passwordEncoder)).isTrue();
    }

    @Test
    @DisplayName("이메일에 대문자가 있으면 400 - 소문자로 바꿔주지 않고 알려준다")
    void signupRejectsUppercaseEmail() throws Exception {
        perform400("Tiger@ibslab.com", "김성호", "password1234", "이메일은 소문자로 입력해 주세요.");
    }

    @Test
    @DisplayName("이메일은 입력한 값 그대로 저장된다")
    void signupKeepsEmailAsTyped() throws Exception {
        perform201("tiger@ibslab.com", "김성호", "password1234");

        User saved = users.findByEmail("tiger@ibslab.com").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("tiger@ibslab.com");
    }

    @Test
    @DisplayName("이름 앞뒤에 공백이 있으면 400 - 몰래 다듬지 않고 알려준다")
    void signupRejectsPaddedName() throws Exception {
        perform400("tiger@ibslab.com", "  김성호  ", "password1234",
                "이름 앞뒤에 공백을 넣을 수 없습니다.");
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 409 와 EMAIL_ALREADY_EXISTS")
    void signupRejectsDuplicateEmail() throws Exception {
        perform201("tiger@ibslab.com", "김성호", "password1234");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("tiger@ibslab.com", "다른이름", "otherpassword")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
    }

    @Test
    @DisplayName("ibslab.com 이 아닌 도메인은 400")
    void signupRejectsForeignDomain() throws Exception {
        perform400("tiger@gmail.com", "김성호", "password1234", "ibslab.com 계정만 가입할 수 있습니다.");
    }

    @Test
    @DisplayName("이름이 1자면 400")
    void signupRejectsShortName() throws Exception {
        perform400("tiger@ibslab.com", "김", "password1234", "이름은 2자 이상 10자 이하여야 합니다.");
    }

    @Test
    @DisplayName("이름이 11자면 400")
    void signupRejectsLongName() throws Exception {
        perform400("tiger@ibslab.com", "가나다라마바사아자차카", "password1234",
                "이름은 2자 이상 10자 이하여야 합니다.");
    }

    @Test
    @DisplayName("비밀번호가 7자면 400, 공백만 8자는 통과한다")
    void signupChecksPasswordLengthOnly() throws Exception {
        perform400("tiger@ibslab.com", "김성호", "pass123", "비밀번호는 8자 이상 20자 이하여야 합니다.");

        perform201("tiger@ibslab.com", "김성호", "        ");
    }

    @Test
    @DisplayName("필드가 null 이면 400")
    void signupRejectsNullFields() throws Exception {
        perform400("tiger@ibslab.com", "김성호", null, "비밀번호를 입력해 주세요.");
        perform400("tiger@ibslab.com", null, "password1234", "이름을 입력해 주세요.");
    }
}
