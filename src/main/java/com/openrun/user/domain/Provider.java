package com.openrun.user.domain;

public enum Provider {
    LOCAL("LOCAL", "일반 로그인"),
    SOCIAL("SOCIAL", "소셜 로그인");

    private final String key;
    private final String title;

    Provider(String key, String title) {
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
