package com.openrun.store.domain;

public enum AcceptStatus {
    OPEN ("OPEN", "접수중"),
    CLOSED("CLOSED", "접수마감");

    private final String key;
    private final String title;

    AcceptStatus(String key, String title) {
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
