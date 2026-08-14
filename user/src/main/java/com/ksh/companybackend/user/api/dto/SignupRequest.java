package com.ksh.companybackend.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Pattern(regexp = "^[^A-Z]*$", message = "이메일은 소문자로 입력해 주세요.")
        @Pattern(regexp = "(?i)^[^@\\s]+@ibslab\\.com$", message = "ibslab.com 계정만 가입할 수 있습니다.")
        String email,

        @NotBlank(message = "이름을 입력해 주세요.")
        @Pattern(regexp = "^\\S(.*\\S)?$", message = "이름 앞뒤에 공백을 넣을 수 없습니다.")
        @Size(min = 2, max = 10, message = "이름은 2자 이상 10자 이하여야 합니다.")
        String name,

        @NotNull(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password) {
}
