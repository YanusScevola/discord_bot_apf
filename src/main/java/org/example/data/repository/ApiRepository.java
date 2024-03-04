package org.example.data.repository;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import net.dv8tion.jda.api.interactions.InteractionHook;
import org.example.data.source.ApiService;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ApiRepository {
    private final ApiService apiService;

    public ApiRepository(JDA jda) {
        this.apiService = ApiService.getInstance(jda);
    }

    public TextChannel getTextChannel(long channelId) {
        return apiService.getTextChannel(channelId);
    }

    public Guild getGuild() {
        return apiService.getGuild();
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

    public void addRolesToMembers(Map<Member, Long> memberToRoleMap, Runnable callback) {
        apiService.addRoleToMembers(memberToRoleMap, callback);
    }

    public void removeRoleFromUsers(Map<Member, Long> memberToRoleMap, Runnable callback) {
        apiService.removeRoleFromUsers(memberToRoleMap, callback);
    }

    public void moveMembers(List<Member> members, VoiceChannel targetChannel, Runnable callback) {
        apiService.moveMembers(members, targetChannel, callback);
    }

    public void disabledMicrophone(List<Member> members, Runnable callback) {
        apiService.processingMicrophone(members, true, callback);
    }
    public void enabledMicrophone(List<Member> members,Runnable callback) {
        apiService.processingMicrophone(members, false, callback);
    }

    public void showEphemeralLoading(@NotNull ButtonInteractionEvent event, Consumer<InteractionHook> callback) {
        apiService.showEphemeralLoading(event, callback);
    }

    public void deleteVoiceChannels(List<VoiceChannel> channels, Runnable callback) {
        apiService.deleteVoiceChannels(channels, callback);
    }
}