package com.ksh.companybackend.calendar.domain;

public enum LeaveKind {

    ANNUAL("연차"),
    HALF_DAY_AM("오전반차"),
    HALF_DAY_PM("오후반차"),
    OFFICIAL("공가");

    private final String label;

    LeaveKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public boolean isHalfDay() {
        return this == HALF_DAY_AM || this == HALF_DAY_PM;
    }
}
