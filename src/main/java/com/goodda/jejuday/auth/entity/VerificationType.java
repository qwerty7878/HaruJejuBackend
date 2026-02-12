package com.goodda.jejuday.auth.entity;

public enum VerificationType {
    SIGNUP("회원가입"),
    PASSWORD_RESET("비밀번호 재설정");

    private final String description;

    VerificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}