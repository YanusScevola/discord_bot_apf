package org.example.data.models;

import java.time.LocalDateTime;
import java.util.List;

public class DebateModel {
    private long id;
    private int themeId;
    private List<Long> governmentMembersIds;
    private List<Long> oppositionMembersIds;
    private LocalDateTime startDateTime;
    private boolean isGovernmentWinner;

    public DebateModel() {
    }

    public DebateModel(long id, List<Long> convertStringToListId, List<Long> convertStringToListId1, LocalDateTime toLocalDateTime, boolean aBoolean) {
        this.id = id;
        this.governmentMembersIds = convertStringToListId;
        this.oppositionMembersIds = convertStringToListId1;
        this.startDateTime = toLocalDateTime;
        this.isGovernmentWinner = aBoolean;
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

    @Override
    public String toString() {
        return "DebateAPF{" +
                "id='" + id + '\'' +
                ", themeId=" + themeId +
                ", governmentUsersIds=" + governmentMembersIds +
                ", oppositionUsersIds=" + oppositionMembersIds +
                ", dateTime=" + startDateTime +
                ", isGovernmentWinner=" + isGovernmentWinner +
                '}';
    }
}