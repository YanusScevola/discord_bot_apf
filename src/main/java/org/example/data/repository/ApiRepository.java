package org.example.data.repository;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import org.example.data.source.ApiService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class ApiRepository {
    private final ApiService apiService;

    public ApiRepository(JDA jda) {
        this.apiService = ApiService.getInstance(jda);
    }

    public TextChannel getTextChannel(String channelId) {
        return apiService.getTextChannel(channelId);
    }

    public CompletableFuture<List<Member>> getMembersByRole(long roleId) {
        return apiService.getMembersByRole(roleId);
    }

    public CompletableFuture<Message> getMessageByIndex(TextChannel channel, int index) {
        return apiService.getMessageByIndex(channel, index);
    }

    public CompletableFuture<Category> getCategoryByID(long id) {
        return apiService.getCategoryByID(id);
    }
    public CompletableFuture<Role> getRoleByID(long id) {
        return apiService.getRoleByID(id);
    }

    public CompletableFuture<Void> addRoleToUser(String userId, long roleId) {
        return apiService.addRoleToUser(userId, roleId);
    }
    public CompletableFuture<Void> removeRoleFromUser(String userId, long roleId) {
        return apiService.removeRoleFromUser(userId, roleId);
    }

    public CompletableFuture<Void> moveMembersAsync(Set<Member> members, VoiceChannel targetChannel) {
        return apiService.moveMembersAsync(members, targetChannel);
    }

    public void showEphemeralMessage(@NotNull ButtonInteractionEvent event, String message) {
        apiService.showEphemeralMessage(event, message);
    }

}