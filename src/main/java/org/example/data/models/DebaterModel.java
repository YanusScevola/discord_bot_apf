package org.example.data.models;

import java.util.List;

public class DebaterModel {
    private long memberId;
    private String nickname;
    private String serverNickname;
    private List<Long> debatesIds;
    private int lossesDebatesCount;
    private int winnDebatesCount;

    public DebaterModel() {}

    public DebaterModel(long aLong, String nickname, String serverNickname, List<Long> convertStringToListId, int anInt, int anInt1) {
        this.memberId = aLong;
        this.nickname = nickname;
        this.serverNickname = serverNickname;
        this.debatesIds = convertStringToListId;
        this.lossesDebatesCount = anInt;
        this.winnDebatesCount = anInt1;
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

    public String getServerNickname() {
        return serverNickname;
    }

    public void setServerNickname(String serverNickname) {
        this.serverNickname = serverNickname;
    }

    public List<Long> getDebatesIds() {
        return debatesIds;
    }

    public void setDebatesIds(List<Long> debatesIds) {
        this.debatesIds = debatesIds;
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

    @Override
    public String toString() {
        return "DebaterAPF{" +
                "memberId=" + memberId +
                ", nickname='" + serverNickname + '\'' +
                ", debatesIds=" + debatesIds +
                ", lostDebatesCount=" + lossesDebatesCount +
                ", winnDebatesCount=" + winnDebatesCount +
                '}';
    }
}
