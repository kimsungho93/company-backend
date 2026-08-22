package com.ksh.companybackend.game.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoomCreateRequest(
        @NotBlank(message = "방 이름을 입력해 주세요.")
        @Size(max = 29, message = "방 이름은 29자까지 입력할 수 있습니다.")
        String name,

        String password) {

    // 검증 전에 다듬는다. 애노테이션은 여기서 정리된 값을 본다.
    public RoomCreateRequest {
        name = name == null ? null : name.trim();
    }
}
