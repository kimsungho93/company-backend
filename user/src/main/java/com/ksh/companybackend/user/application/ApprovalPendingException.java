package com.ksh.companybackend.user.application;

import com.ksh.companybackend.common.error.BusinessException;

public class ApprovalPendingException extends BusinessException {

    public ApprovalPendingException() {
        super("APPROVAL_PENDING", 403, "관리자 승인 대기 중입니다.");
    }
}
