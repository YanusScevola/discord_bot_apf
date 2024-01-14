package org.example.core.models;

import java.util.List;

public class Team {
    private String id;
    private String name;
    private List<Debater> debaters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Debater> getDebaters() {
        return debaters;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDebaters(List<Debater> debaters) {
        this.debaters = debaters;
    }
}
