package org.example.core.models;

import net.dv8tion.jda.api.entities.Member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Debate {
    private long id;
    private Theme theme;
    private List<Member> governmentDebaters = new ArrayList<>();
    private List<Member> judges = new ArrayList<>();
    private List<Member> oppositionDebaters = new ArrayList<>();
    private LocalDateTime endDateTime;
    private boolean isGovernmentWinner;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
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

    public List<Member> getJudges() {
        return judges;
    }

    public void setJudges(List<Member> judges) {
        this.judges = judges;
    }
}
