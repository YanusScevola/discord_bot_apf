package org.example.core.models;

import net.dv8tion.jda.api.entities.Member;

import java.sql.Timestamp;

public class AwaitingTestUser {
    private Member member;
    private String testName;
    private Timestamp time;

    public AwaitingTestUser(Member member, String testName, Timestamp time) {
        this.member = member;
        this.testName = testName;
        this.time = time;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
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
