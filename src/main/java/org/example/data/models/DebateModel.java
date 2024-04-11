package org.example.data.models;

import java.time.LocalDateTime;
import java.util.List;

public class DebateModel {
    private long id;
    private int themeId;
    private List<Long> governmentMembersIds;
    private List<Long> judgesIds;
    private List<Long> oppositionMembersIds;
    private LocalDateTime startDateTime;
    private boolean isGovernmentWinner;

    public DebateModel() {
    }

    public DebateModel(long id, int themeId, List<Long> governmentMembersIds, List<Long> judgesIds, List<Long> oppositionMembersIds, LocalDateTime startDateTime, boolean isGovernmentWinner) {
        this.id = id;
        this.themeId = themeId;
        this.governmentMembersIds = governmentMembersIds;
        this.judgesIds = judgesIds;
        this.oppositionMembersIds = oppositionMembersIds;
        this.startDateTime = startDateTime;
        this.isGovernmentWinner = isGovernmentWinner;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getThemeId() {
        return themeId;
    }

    public void setThemeId(int themeId) {
        this.themeId = themeId;
    }

    public List<Long> getGovernmentMembersIds() {
        return governmentMembersIds;
    }

    public void setGovernmentMembersIds(List<Long> governmentMembersIds) {
        this.governmentMembersIds = governmentMembersIds;
    }

    public List<Long> getOppositionMembersIds() {
        return oppositionMembersIds;
    }

    public void setOppositionMembersIds(List<Long> oppositionMembersIds) {
        this.oppositionMembersIds = oppositionMembersIds;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public boolean isGovernmentWinner() {
        return isGovernmentWinner;
    }

    public void setGovernmentWinner(boolean governmentWinner) {
        isGovernmentWinner = governmentWinner;
    }

    public List<Long> getJudgesIds() {
        return judgesIds;
    }

    public void setJudgesIds(List<Long> judgesIds) {
        this.judgesIds = judgesIds;
    }

    @Override
    public String toString() {
        return "DebateAPF{" +
                "id='" + id + '\'' +
                ", themeId=" + themeId +
                ", governmentUsersIds=" + governmentMembersIds +
                ", judgesIds=" + judgesIds +
                ", oppositionUsersIds=" + oppositionMembersIds +
                ", dateTime=" + startDateTime +
                ", isGovernmentWinner=" + isGovernmentWinner +
                '}';
    }
}