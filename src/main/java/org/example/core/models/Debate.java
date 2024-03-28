package org.example.core.models;

import net.dv8tion.jda.api.entities.Member;

import java.time.LocalDateTime;
import java.util.List;

public class Debate {
    private long id;
    private List<Member> governmentDebaters;
    private List<Member> oppositionDebaters;
    private LocalDateTime endDateTime;
    private boolean isGovernmentWinner;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<Member> getGovernmentDebaters() {
        return governmentDebaters;
    }

    public void setGovernmentDebaters(List<Member> governmentDebaters) {
        this.governmentDebaters = governmentDebaters;
    }

    public List<Member> getOppositionDebaters() {
        return oppositionDebaters;
    }

    public void setOppositionDebaters(List<Member> oppositionDebaters) {
        this.oppositionDebaters = oppositionDebaters;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public boolean isGovernmentWinner() {
        return isGovernmentWinner;
    }

    public void setIsGovernmentWinner(boolean governmentWinner) {
        isGovernmentWinner = governmentWinner;
    }
}
