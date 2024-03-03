package org.example.core.models;

import java.util.List;

public class DebaterAPF {
    private long memberId;
    private String nickname;
    private List<Integer> debatesIds;
    private int lostDebatesCount;
    private int winnDebatesCount;
    private int speakingPoints;

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

    public List<Integer> getDebatesIds() {
        return debatesIds;
    }

    public void setDebatesIds(List<Integer> debatesIds) {
        this.debatesIds = debatesIds;
    }

    public int getLostDebatesCount() {
        return lostDebatesCount;
    }

    public void setLostDebatesCount(int lostDebatesCount) {
        this.lostDebatesCount = lostDebatesCount;
    }

    public int getWinnDebatesCount() {
        return winnDebatesCount;
    }

    public void setWinnDebatesCount(int winnDebatesCount) {
        this.winnDebatesCount = winnDebatesCount;
    }

    public int getSpeakingPoints() {
        return speakingPoints;
    }

    public void setSpeakingPoints(int speakingPoints) {
        this.speakingPoints = speakingPoints;
    }

    @Override
    public String toString() {
        return "DebaterAPF{" +
                "memberId=" + memberId +
                ", nickname='" + nickname + '\'' +
                ", debatesIds=" + debatesIds +
                ", lostDebatesCount=" + lostDebatesCount +
                ", winnDebatesCount=" + winnDebatesCount +
                ", speakingPoints=" + speakingPoints +
                '}';
    }
}
