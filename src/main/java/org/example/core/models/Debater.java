package org.example.core.models;

import java.util.List;

public class Debater {
    private long memberId;
    private String nickname;
    private String serverNickname;
    private List<Debate> debates;
    private int lossesCount;
    private int winnCount;

    public List<Debate> getDebates() {
        return debates;
    }

    public void setDebates(List<Debate> debates) {
        this.debates = debates;
    }

    public String getServerNickname() {
        return serverNickname;
    }

    public void setServerNickname(String serverNickname) {
        this.serverNickname = serverNickname;
    }

    public int getLossesCount() {
        return lossesCount;
    }

    public void setLossesCount(int lossesDebatesCount) {
        this.lossesCount = lossesDebatesCount;
    }

    public int getWinnCount() {
        return winnCount;
    }

    public void setWinnCount(int winnDebatesCount) {
        this.winnCount = winnDebatesCount;
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
