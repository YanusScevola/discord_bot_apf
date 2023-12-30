package org.example.data.source.models;

public class DebateSourceModel {
    private String id;
    private String name;
    private String thesis;
    private String dateString;
    private String governmentTeamId;
    private String oppositionTeamId;
    private int winner;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThesis() {
        return thesis;
    }

    public void setThesis(String thesis) {
        this.thesis = thesis;
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public String getGovernmentTeamId() {
        return governmentTeamId;
    }

    public void setGovernmentTeamId(String governmentTeamId) {
        this.governmentTeamId = governmentTeamId;
    }

    public String getOppositionTeamId() {
        return oppositionTeamId;
    }

    public void setOppositionTeamId(String oppositionTeamId) {
        this.oppositionTeamId = oppositionTeamId;
    }

    public int getWinner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }
}
