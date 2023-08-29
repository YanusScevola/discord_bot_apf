package org.example.repository;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.constants.RoleIds;
import org.example.models.Debater;
import org.example.source.ApiService;
import org.example.source.Database;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class DbRepository {

    private final Database dataBase;

    public DbRepository() {
        this.dataBase = Database.getInstance();
    }


    public List<Debater> getAllDebaters() {
        return dataBase.readAllDebaters();
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
