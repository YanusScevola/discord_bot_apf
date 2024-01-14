package org.example.core.models;

public class Debate {
    private String id;
    private String thesis;
    private String date;
    private Team governmentTeam;
    private Team oppositionTeam;
    private int winner;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getThesis() {
        return thesis;
    }

    public void setThesis(String thesis) {
        this.thesis = thesis;
    }

    public Team getGovernmentTeam() {
        return governmentTeam;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public void setGovernmentTeam(Team governmentTeam) {
        this.governmentTeam = governmentTeam;
    }

    public Team getOppositionTeam() {
        return oppositionTeam;
    }

    public void setOppositionTeam(Team oppositionTeam) {
        this.oppositionTeam = oppositionTeam;
    }

    public int getWinner() {
        return winner;
    }

    public void setWinner(int winner) {
        this.winner = winner;
    }
}