package org.example.data.source;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.ui.constants.ServerID;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ApiService {
    private static JDA jda;
    private static Guild server;


    private ApiService() {
    }

    private static class SingletonHolder {
        private static final ApiService INSTANCE = new ApiService();
    }

    public static ApiService getInstance(JDA jda2) {
        jda = jda2;

        server = jda.getGuildById(ServerID.SERVER_ID);
        return SingletonHolder.INSTANCE;
    }

    public TextChannel getTextChannel(String channelId) {
        return jda.getTextChannelById(channelId);
    }

    public CompletableFuture<List<Member>> getAllMembers() {
        CompletableFuture<List<Member>> future = new CompletableFuture<>();
        if (server == null) {
            future.completeExceptionally(new IllegalArgumentException("Нет такого сервера"));
            return future;
        }
        server.loadMembers()
                .onSuccess(future::complete)
                .onError(e -> future.completeExceptionally(new RuntimeException("Ошибка загрузки участников")));
        return future;
    }

    public CompletableFuture<List<Member>> getMembersByRole(String roleId) {
        return CompletableFuture.supplyAsync(() -> {
            CompletableFuture<List<Member>> internalFuture = new CompletableFuture<>();
            if (server == null) {
                internalFuture.completeExceptionally(new IllegalArgumentException("Нет такого сервера"));
                return Collections.emptyList();
            }
            Role role = server.getRoleById(roleId);
            if (role == null) {
                internalFuture.completeExceptionally(new IllegalArgumentException("Нет такой роли"));
                return Collections.emptyList();
            }
            server.loadMembers()
                    .onSuccess(members -> {
                        List<Member> filteredMembers = members.stream()
                                .filter(member -> member.getRoles().contains(role))
                                .collect(Collectors.toList());
                        internalFuture.complete(filteredMembers);
                    })
                    .onError(e ->
                            internalFuture.completeExceptionally(new RuntimeException("Ошибка загрузки участников", e))
                    );
            try {
                return internalFuture.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }


    public CompletableFuture<Message> getMessageByIndex(TextChannel channel, int index) {
        CompletableFuture<Message> future = new CompletableFuture<>();

        channel.getHistory().retrievePast(index + 1).queue((messages) -> {
            if (messages.size() > index) {
                future.complete(messages.get(index));
            } else {
                future.completeExceptionally(new IllegalArgumentException("Недостаточно сообщений в канале"));
            }
        }, future::completeExceptionally);

        return future;
    }


    public CompletableFuture<Category> getCategoryByID(String id) {
        CompletableFuture<Category> future = new CompletableFuture<>();
        if (server == null) {
            future.completeExceptionally(new IllegalArgumentException("Нет такого сервера"));
            return future;
        }
        Category category = server.getCategoryById(id);
        if (category == null) {
            future.completeExceptionally(new IllegalArgumentException("Нет такой категории"));
            return future;
        }
        future.complete(category);
        return future;
    }

    public CompletableFuture<Void> assignRoleToUser(String userId, String roleId) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        if (server == null) {
            future.completeExceptionally(new IllegalArgumentException("Нет такого сервера"));
            return future;
        }

        Role role = server.getRoleById(roleId);
        if (role == null) {
            future.completeExceptionally(new IllegalArgumentException("Нет такой роли: " + roleId));
            return future;
        }

        server.retrieveMemberById(userId).queue(member -> {
            server.addRoleToMember(member, role).queue(
                    error -> future.completeExceptionally(new IllegalArgumentException("Ошибка при присвоении роли: " + error)) // Ошибка при присвоении роли
            );
        }, error -> future.completeExceptionally(new IllegalArgumentException("Пользователь с таким ID не найден: " + error.getMessage())));

        return future;
    }





}
