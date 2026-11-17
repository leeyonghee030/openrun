package com.openrun.user.domain;

public enum UserStatus {
    ACTIVE("ACTIVE", "활성화"),
    WITHDRAWN("WITHDRAWN", "비활성화");

    private final String key;
    private final String title;

    UserStatus(String key, String title) {
        this.key = key;
        this.title = title;
    }

    public String getKey() {
        return key;
    }
    public String getTitle() {
        return title;
    }
}
