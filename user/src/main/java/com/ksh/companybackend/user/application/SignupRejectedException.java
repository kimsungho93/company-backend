package com.ksh.companybackend.user.application;

import com.ksh.companybackend.common.error.BusinessException;

public class SignupRejectedException extends BusinessException {

    public SignupRejectedException() {
        super("SIGNUP_REJECTED", 403, "가입이 거절되었습니다. 관리자에게 문의해 주세요.");
    }
}
