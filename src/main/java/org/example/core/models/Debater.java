package org.example.core.models;

import java.util.List;

public class Debater {
    private long memberId;
    private String nickname;
    private List<Debate> debates;
    private int lossesDebatesCount;
    private int winnDebatesCount;

    public List<Debate> getDebates() {
        return debates;
    }

    public void setDebates(List<Debate> debates) {
        this.debates = debates;
    }

    public int getLossesDebatesCount() {
        return lossesDebatesCount;
    }

    public void setLossesDebatesCount(int lossesDebatesCount) {
        this.lossesDebatesCount = lossesDebatesCount;
    }

    public int getWinnDebatesCount() {
        return winnDebatesCount;
    }

    public void setWinnDebatesCount(int winnDebatesCount) {
        this.winnDebatesCount = winnDebatesCount;
    }

    public long getMemberId() {
        return memberId;
    }

    public void setMemberId(long memberId) {
        this.memberId = memberId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
