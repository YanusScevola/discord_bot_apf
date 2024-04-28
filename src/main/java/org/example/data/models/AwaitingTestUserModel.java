package org.example.data.models;

import java.sql.Timestamp;

public class AwaitingTestUserModel {
    private long userId;
    private String testName;
    private Timestamp time;

    public AwaitingTestUserModel(long userId, String testName, Timestamp time) {
        this.userId = userId;
        this.testName = testName;
        this.time = time;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public Timestamp getTime() {
        return time;
    }

    public void setTime(Timestamp time) {
        this.time = time;
    }
}
