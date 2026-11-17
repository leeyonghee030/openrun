package com.openrun.waiting.domain;

public enum WaitingStatus {
    WAITING("WAITING", "대기중"),
    CALLED("CALLED", "호출됨"),
    ENTERED("ENTERED", "입장완료"),
    CANCELED("CANCELED", "취소됨"),
    NO_SHOW("NO_SHOW", "노쇼");

    private final String key;
    private final String title;

    WaitingStatus(String key, String title) {
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
