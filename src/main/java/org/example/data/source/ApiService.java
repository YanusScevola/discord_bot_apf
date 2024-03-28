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
import org.example.core.constants.ServerID;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
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

    public Guild getGuild() {
        return jda.getGuildById(ServerID.SERVER_ID);
    }

    public CompletableFuture<List<Member>> getMembersByIds(List<Long> memberIds) {
        CompletableFuture<List<Member>> resultFuture = new CompletableFuture<>();
        List<Member> members = new ArrayList<>();

        if (server == null) {
            resultFuture.completeExceptionally(new IllegalArgumentException("Нет такого сервера"));
            return resultFuture;
        }

        if (memberIds.isEmpty()) {
            resultFuture.complete(members); // Пустой список, если нет ID для поиска
            return resultFuture;
        }

        CompletableFuture<?>[] futures = new CompletableFuture[memberIds.size()];

        for (int i = 0; i < memberIds.size(); i++) {
            final int index = i;
            Long memberId = memberIds.get(index);
            CompletableFuture<Member> memberFuture = new CompletableFuture<>();
            futures[i] = memberFuture;

            // Преобразуем Long ID обратно в String для retrieveMemberById
            server.retrieveMemberById(memberId.toString()).queue(
                    member -> {
                        synchronized (members) {
                            members.add(member);
                        }
                        memberFuture.complete(member);
                    },
                    failure -> {
                        System.err.println("Не удалось получить информацию о участнике с ID: " + memberId);
                        memberFuture.completeExceptionally(failure);
                    }
            );
        }

        CompletableFuture.allOf(futures).thenRun(() -> {
            resultFuture.complete(members);
        }).exceptionally(e -> {
            resultFuture.completeExceptionally(e);
            return null;
        });

        return resultFuture;
    }

    public CompletableFuture<Member> getMemberByRole(long roleId) {
        CompletableFuture<Member> resultFuture = new CompletableFuture<>();

        if (server == null) {
            resultFuture.completeExceptionally(new IllegalArgumentException("Нет такого сервера"));
            return resultFuture;
        }

        Role role = server.getRoleById(roleId);
        if (role == null) {
            resultFuture.completeExceptionally(new IllegalArgumentException("Нет такой роли"));
            return resultFuture;
        }

        server.loadMembers().onSuccess(members -> {
            Member member = members.stream()
                    .filter(m -> m.getRoles().contains(role))
                    .findFirst()
                    .orElse(null);
            if (member != null) {
                resultFuture.complete(member);
            } else {
                resultFuture.completeExceptionally(new IllegalArgumentException("Нет участников с такой ролью"));
            }
        }).onError(e -> resultFuture.completeExceptionally(new RuntimeException("Ошибка загрузки участников", e)));

        return resultFuture;
    }

    public CompletableFuture<List<Member>> getMembersByRole(long roleId) {
        CompletableFuture<List<Member>> resultFuture = new CompletableFuture<>();

        if (server == null) {
            resultFuture.completeExceptionally(new IllegalArgumentException("Нет такого сервера"));
            return resultFuture;
        }

        Role role = server.getRoleById(roleId);
        if (role == null) {
            resultFuture.completeExceptionally(new IllegalArgumentException("Нет такой роли"));
            return resultFuture;
        }

        server.loadMembers().onSuccess(members -> {
            List<Member> filteredMembers = members.stream()
                    .filter(member -> member.getRoles().contains(role))
                    .collect(Collectors.toList());
            resultFuture.complete(filteredMembers);
        }).onError(e -> resultFuture.completeExceptionally(new RuntimeException("Ошибка загрузки участников", e)));

        return resultFuture;
    }

    public CompletableFuture<Message> getMessageByIndex(TextChannel channel, int index) {
        CompletableFuture<Message> resultFuture = new CompletableFuture<>();

        channel.getHistory().retrievePast(index + 1).queue(messages -> {
            if (messages.size() > index) {
                resultFuture.complete(messages.get(index));
            } else {
                resultFuture.completeExceptionally(new IllegalArgumentException("Недостаточно сообщений в канале"));
            }
        }, error -> {
            if (error instanceof Exception) {
                resultFuture.completeExceptionally((Exception) error);
            } else {
                resultFuture.completeExceptionally(new Exception(error));
            }
        });

        return resultFuture;
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

    public CompletableFuture<Boolean> addRoleToMembers(Map<Member, Long> memberToRoleMap) {
        if (memberToRoleMap == null || memberToRoleMap.isEmpty()) {
            System.err.println("Словарь участников и ролей пуст или не предоставлен.");
            return CompletableFuture.completedFuture(true); // Предполагаем, что пустой список - это успех
        }

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (Map.Entry<Member, Long> entry : memberToRoleMap.entrySet()) {
            Member member = entry.getKey();
            Long roleId = entry.getValue();

            if (member == null) {
                System.err.println("Один из участников является null.");
                futures.add(CompletableFuture.completedFuture(false));
                continue;
            }

            Role role = server.getRoleById(roleId);
            if (role == null) {
                System.err.println("Роль с ID " + roleId + " не найдена на сервере.");
                futures.add(CompletableFuture.completedFuture(false));
                continue;
            }

            CompletableFuture<Boolean> future = new CompletableFuture<>();
            server.addRoleToMember(member, role).queue(
                    success -> future.complete(true),
                    failure -> {
                        System.err.println("Не удалось выдать роль " + role.getName() + " участнику: " + member.getEffectiveName() + ". Ошибка: " + failure.getMessage());
                        future.complete(false);
                    }
            );
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().allMatch(CompletableFuture::join)); // Проверяем, были ли все операции успешны
    }


    public CompletableFuture<Boolean> removeRoleFromUser(String userId, long roleId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
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
                    success -> future.complete(true), // Успешное выполнение
                    error -> future.complete(false) // В случае ошибки возвращаем false, но не исключение
            );
        }, error -> future.completeExceptionally(new IllegalArgumentException("Пользователь с таким ID не найден: " + error.getMessage())));

        return future;
    }


    public CompletableFuture<Boolean> removeRoleFromUsers(Map<Member, Long> memberToRoleMap) {
        if (memberToRoleMap == null || memberToRoleMap.isEmpty()) {
            System.err.println("Словарь участников и ролей пуст или не предоставлен.");
            return CompletableFuture.completedFuture(false); // Изменено на false, так как не было выполнено никаких операций
        }

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (Map.Entry<Member, Long> entry : memberToRoleMap.entrySet()) {
            Member member = entry.getKey();
            Long roleId = entry.getValue();

            if (member == null) {
                System.err.println("Один из участников является null.");
                continue;
            }

            Role role = server.getRoleById(roleId);
            if (role == null) {
                System.err.println("Роль с ID " + roleId + " не найдена на сервере.");
                continue;
            }

            CompletableFuture<Boolean> future = new CompletableFuture<>();
            server.removeRoleFromMember(member, role).queue(
                    success -> future.complete(true), // Операция успешно завершена
                    failure -> {
                        System.err.println("Не удалось удалить роль " + role.getName() + " у участника: " + member.getEffectiveName() + ". Ошибка: " + failure.getMessage());
                        future.complete(false); // Операция завершилась с ошибкой
                    }
            );
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().allMatch(CompletableFuture::join)); // Проверяем, все ли будущие задачи были успешны
    }


    public CompletableFuture<InteractionHook> showEphemeralLoading(@NotNull ButtonInteractionEvent event) {
        CompletableFuture<InteractionHook> resultFuture = new CompletableFuture<>();

        if (!event.isAcknowledged()) {
            event.deferReply(true).queue(
                    hook -> {
                        hook.deleteOriginal().queueAfter(5, TimeUnit.SECONDS,
                                null,
                                failure -> {
                                    System.err.println("Не удалось удалить оригинальное сообщение: " + failure.getMessage());
                                    resultFuture.completeExceptionally(failure);
                                }
                        );
                        resultFuture.complete(hook);
                    },
                    failure -> {
                        System.err.println("Не удалось отложить ответ: " + failure.getMessage());
                        resultFuture.completeExceptionally(failure);
                    }
            );
        } else {
            System.err.println("Взаимодействие уже обработано");
            resultFuture.completeExceptionally(new IllegalStateException("Взаимодействие уже обработано"));
        }

        return resultFuture;
    }

    public CompletableFuture<Boolean> processingMicrophone(List<Member> members, boolean mute) {
        if (members.isEmpty()) {
            return CompletableFuture.completedFuture(true); // Список участников пуст, считаем операцию успешной
        }

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (Member member : members) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            if (member.getVoiceState() != null && member.getVoiceState().inAudioChannel()) {
                member.mute(mute).queue(
                        success -> future.complete(true), // Операция успешно выполнена
                        failure -> {
                            System.err.println("Не удалось отключить микрофон у участника: " + member.getEffectiveName());
                            future.complete(false); // Операция не удалась
                        }
                );
            } else {
                System.err.println("Участник не в голосовом канале: " + member.getEffectiveName());
                future.complete(true); // Считаем, что операция успешна, даже если участник не в канале
            }
            futures.add(future);
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().allMatch(CompletableFuture::join)); // Проверяем, все ли операции были успешны
    }


    public CompletableFuture<Boolean> moveMembers(List<Member> members, VoiceChannel targetChannel) {
        if (members.isEmpty()) {
            return CompletableFuture.completedFuture(true); // Список участников пуст, считаем операцию успешной
        }

        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        for (Member member : members) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            member.getGuild().moveVoiceMember(member, targetChannel).queue(
                    success -> future.complete(true), // Операция успешно выполнена
                    failure -> {
                        System.err.println("Не удалось переместить участника: " + member.getEffectiveName() + ". Причина: " + failure.getMessage());
                        future.complete(false); // Операция не удалась
                    }
            );
            futures.add(future);
        }

        // Объединяем все будущие результаты и проверяем, были ли все операции успешны
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream().allMatch(CompletableFuture::join)); // Возвращаем true, если все операции успешны
    }


    public CompletableFuture<Boolean> deleteVoiceChannels(List<VoiceChannel> channels) {
        if (channels.isEmpty()) {
            return CompletableFuture.completedFuture(true); // Список каналов пуст, считаем операцию успешной
        }

        List<CompletableFuture<Boolean>> futuresList = new ArrayList<>();
        for (VoiceChannel channel : channels) {
            // Преобразовываем каждую операцию удаления в CompletableFuture<Boolean>, отражающий успех операции
            CompletableFuture<Boolean> future = channel.delete().submit()
                    .toCompletableFuture()
                    .thenApply(result -> true) // В случае успеха возвращаем true
                    .exceptionally(ex -> {
                        System.err.println("Не удалось удалить голосовой канал: " + channel.getName() + ", ошибка: " + ex.getMessage());
                        return false; // В случае исключения возвращаем false
                    });
            futuresList.add(future);
        }

        // Преобразуем List в массив CompletableFuture<?> для использования с CompletableFuture.allOf()
        CompletableFuture<Boolean>[] futuresArray = futuresList.toArray(new CompletableFuture[0]);

        return CompletableFuture.allOf(futuresArray)
                .thenApply(v -> futuresList.stream().allMatch(CompletableFuture::join)); // Проверяем, все ли операции были успешны
    }


}
