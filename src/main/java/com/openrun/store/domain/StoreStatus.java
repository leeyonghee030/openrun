package com.openrun.store.domain;

public enum StoreStatus {
    PENDING("PENDING", "대기중"),
    APPROVED("APPROVED", "승인됨"),
    REJECTED("REJECTED", "거부됨");

    private final String key;
    private final String title;

    StoreStatus(String key, String title) {
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
