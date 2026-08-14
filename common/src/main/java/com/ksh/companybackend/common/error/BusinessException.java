package com.ksh.companybackend.common.error;

public abstract class BusinessException extends RuntimeException {

    private final String code;
    private final int status;

    protected BusinessException(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public int getStatus() {
        return status;
    }
}
