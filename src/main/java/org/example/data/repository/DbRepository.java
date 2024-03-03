package org.example.data.repository;

import org.example.core.models.DebateAPF;
import org.example.core.models.DebaterAPF;
import org.example.data.source.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class DbRepository {

    private final Database dataBase;

    public DbRepository() {
        this.dataBase = Database.getInstance();
    }


    public List<DebaterAPF> getAllDebaters() {
        return dataBase.getAllDebaters();
    }

    public List<DebateAPF> getAllDebates() {
        return new ArrayList<>();
    }

    public void insertDebater(DebaterAPF debaterAPF) {
        Executors.newSingleThreadExecutor().submit(() -> {
            dataBase.insertDebater(debaterAPF);
        });
    }

    public void insertDebaters(List<DebaterAPF> debaterAPF) {
        Executors.newSingleThreadExecutor().submit(() -> {
            dataBase.insertDebaters(debaterAPF);
        });
    }

    public void deleteDebater(long id) {
        Executors.newSingleThreadExecutor().submit(() -> {
            dataBase.deleteDebater(id);
        });
    }



}
