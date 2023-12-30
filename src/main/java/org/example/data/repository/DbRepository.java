package org.example.data.repository;

import org.example.ui.models.Debate;
import org.example.ui.models.Debater;
import org.example.data.source.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DbRepository {

    private final Database dataBase;

    public DbRepository() {
        this.dataBase = Database.getInstance();
    }


    public List<Debater> getAllDebaters() {
        return dataBase.getAllDebaters();
    }

    public List<Debate> getAllDebates() {
        return new ArrayList<>();
    }

    public void insertDebater(Debater debater) {
        Executors.newSingleThreadExecutor().submit(() -> {
            dataBase.insertDebater(debater);
        });
    }

    public void insertDebaters(List<Debater> debater) {
        Executors.newSingleThreadExecutor().submit(() -> {
            dataBase.insertDebaters(debater);
        });
    }

    public void deleteDebater(long id) {
        Executors.newSingleThreadExecutor().submit(() -> {
            dataBase.deleteDebater(id);
        });
    }



}
