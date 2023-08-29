package org.example.repository;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.source.ApiService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApiRepository {
    private final ApiService apiService;

    public ApiRepository(JDA jda) {
        this.apiService = ApiService.getInstance(jda);
    }


    public TextChannel getTextChannel(String channelId) {
        return apiService.getTextChannel(channelId);
    }

    public CompletableFuture<List<Member>> getMembersByRole(String roleId) {
        return apiService.getMembersByRole(roleId);
    }

    public CompletableFuture<Message> getMessageByIndex(TextChannel channel, int index) {
        return apiService.getMessageByIndex(channel,index);
    }
}