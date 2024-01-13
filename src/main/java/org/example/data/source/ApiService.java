package org.example.data.source;

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
import org.example.ui.constants.ServerID;
import org.example.utils.Utils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.processing.Completion;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
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

    public TextChannel getTextChannel(long channelId) {
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

    public CompletableFuture<List<Member>> getMembersByRole(long roleId) {
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


    public CompletableFuture<Category> getCategoryByID(long id) {
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

    public CompletableFuture<Role> getRoleByID(long id) {
        CompletableFuture<Role> future = new CompletableFuture<>();
        if (server == null) {
            future.completeExceptionally(new IllegalArgumentException("Нет такого сервера"));
            return future;
        }
        Role role = server.getRoleById(id);
        if (role == null) {
            future.completeExceptionally(new IllegalArgumentException("Нет такой роли"));
            return future;
        }
        future.complete(role);
        return future;
    }

    public void addRoleToMembers(Map<Member, Long> memberToRoleMap, Runnable callback) {
        if (memberToRoleMap == null || memberToRoleMap.isEmpty()) {
            System.err.println("Словарь участников и ролей пуст или не предоставлен.");
            if (callback != null) {
                callback.run();
            }
            return;
        }

        final int[] totalOperations = {0};
        final int[] completedOperations = {0};

        // Устанавливаем общее количество операций на размер словаря.
        totalOperations[0] = memberToRoleMap.size();

        for (Map.Entry<Member, Long> entry : memberToRoleMap.entrySet()) {
            Member member = entry.getKey();
            Long roleId = entry.getValue();

            if (member == null) {
                System.err.println("Один из участников является null.");
                completedOperations[0]++;
                continue;
            }

            Role role = server.getRoleById(roleId);
            if (role == null) {
                System.err.println("Роль с ID " + roleId + " не найдена на сервере.");
                completedOperations[0]++;
                continue;
            }

            server.addRoleToMember(member, role).queue(
                    success -> {
                        completedOperations[0]++;
                        if (completedOperations[0] == totalOperations[0] && callback != null) {
                            callback.run();
                        }
                    },
                    failure -> {
                        System.err.println("Не удалось выдать роль " + role.getName() + " участнику: " + member.getEffectiveName() + ". Ошибка: " + failure.getMessage());
                        completedOperations[0]++;
                        if (completedOperations[0] == totalOperations[0] && callback != null) {
                            callback.run();
                        }
                    }
            );
        }
    }



    public CompletableFuture<Void> removeRoleFromUser(String userId, long roleId) {
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
            server.removeRoleFromMember(member, role).queue(
                    error -> future.completeExceptionally(new IllegalArgumentException("Ошибка при удалении роли: " + error)) // Ошибка при удалении роли
            );
        }, error -> future.completeExceptionally(new IllegalArgumentException("Пользователь с таким ID не найден: " + error.getMessage())));

        return future;
    }


    public void showEphemeralLoading(@NotNull ButtonInteractionEvent event, Consumer<InteractionHook> callback) {
        if (!event.isAcknowledged()) {
            event.deferReply(true).queue(
                    hook -> {
                        hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS);
                        callback.accept(hook);
                    },
                    failure -> System.err.println("Не удалось отложить ответ: " + failure.getMessage())
            );
        } else {
            System.err.println("Взаимодействие уже обработано");
        }
    }



    public void muteMembers(List<Member> members, boolean mute, Runnable callback) {
        try {
            if (members.isEmpty()) {
                callback.run();
                return;
            }

            final int[] counter = {0};

            for (Member member : members) {
                if (member.getVoiceState() != null && member.getVoiceState().inAudioChannel()) {
                    member.mute(mute).queue(
                            success -> {
                                if (++counter[0] == members.size() && callback != null) {
                                    callback.run();
                                }
                            },
                            failure -> {
                                System.err.println("Не удалось отключить микрофон у участника: " + member.getEffectiveName());
                                if (++counter[0] == members.size() && callback != null) {
                                    callback.run();
                                }
                            }
                    );
                } else {
                    System.err.println("Участник не в голосовом канале: " + member.getEffectiveName());
                    if (++counter[0] == members.size() && callback != null) {
                        callback.run();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void moveMembers(List<Member> members, VoiceChannel targetChannel, Runnable callback) {
        try {
            if (members.isEmpty()) {
                callback.run();
                return;
            }

            final int[] counter = {0};

            for (Member member : members) {
                member.getGuild().moveVoiceMember(member, targetChannel).queue(
                        success -> {
                            if (++counter[0] == members.size()) {
                                callback.run();
                            }
                        },
                        failure -> {
                            System.err.println("Не удалось переместить участника: " + member.getEffectiveName());
                            if (++counter[0] == members.size()) {
                                callback.run();
                            }
                        }
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void deleteVoiceChannels(List<VoiceChannel> channels, Runnable callback) {
        CompletableFuture<Void>[] futures = new CompletableFuture[channels.size()];

        for (int i = 0; i < channels.size(); i++) {
            VoiceChannel channel = channels.get(i);
            CompletableFuture<Void> future = new CompletableFuture<>();
            futures[i] = future;

            channel.delete().queue(
                    success -> future.complete(null),
                    error -> {
                        System.err.println("Ошибка при удалении канала: " + channel.getName() + "; причина: " + error.getMessage());
                        future.completeExceptionally(error);
                    }
            );
        }

        CompletableFuture.allOf(futures).thenRun(callback);
    }


}
