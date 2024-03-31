package org.example.domain;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.example.core.models.Debate;
import org.example.core.models.Debater;
import org.example.core.models.Question;
import org.example.core.models.Theme;
import org.example.data.models.DebateModel;
import org.example.data.models.DebaterModel;
import org.example.data.models.ThemeModel;
import org.example.data.source.ApiService;
import org.example.data.source.db.DbOperations;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class UseCase {

    private static UseCase instance;

    private final DbOperations dataBase;
    private final ApiService apiService;

    private UseCase(ApiService apiService, DbOperations database) {
        this.apiService = apiService;
        this.dataBase = database;
    }

    public static synchronized UseCase getInstance(ApiService apiService, DbOperations database) {
        if (instance == null) {
            instance = new UseCase(apiService, database);
        }
        return instance;
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

    public CompletableFuture<Member> getMemberByRole(long roleId) {
        return apiService.getMemberByRole(roleId);
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

    public CompletableFuture<Boolean> addRolesToMembers(Map<Member, Long> memberToRoleMap) {
        return apiService.addRoleToMembers(memberToRoleMap);
    }

    public CompletableFuture<Boolean> removeRoleFromUsers(Map<Member, Long> memberToRoleMap) {
        return apiService.removeRoleFromUsers(memberToRoleMap);
    }

    public CompletableFuture<Boolean> moveMembers(List<Member> members, VoiceChannel targetChannel) {
        return apiService.moveMembers(members, targetChannel);
    }

    public CompletableFuture<Boolean> disabledMicrophone(List<Member> members) {
        return apiService.processingMicrophone(members, true);
    }

    public CompletableFuture<Boolean> enabledMicrophone(List<Member> members) {
        return apiService.processingMicrophone(members, false);
    }

    public CompletableFuture<InteractionHook> showEphemeralLoading(@NotNull ButtonInteractionEvent event) {
        return apiService.showEphemeralLoading(event);
    }

    public CompletableFuture<Boolean> deleteVoiceChannels(List<VoiceChannel> channels) {
        return apiService.deleteVoiceChannels(channels);
    }

    public CompletableFuture<Theme> getTheme(int themeId) {
        return dataBase.getTheme(themeId).thenApply(themeModel -> {
            Theme theme = new Theme();
            theme.setId(themeModel.getId());
            theme.setName(themeModel.getName());
            theme.setUsageCount(themeModel.getUsageCount());
            return theme;
        });
    }

    public CompletableFuture<Theme> getRandomTheme() {
        return dataBase.getRandomTheme().thenApply(themeModel -> {
            Theme theme = new Theme();
            theme.setId(themeModel.getId());
            theme.setName(themeModel.getName());
            theme.setUsageCount(themeModel.getUsageCount());
            return theme;
        });
    }

    public CompletableFuture<List<Debater>> getAllDebaters() {
        CompletableFuture<List<Debater>> result = new CompletableFuture<>();

        dataBase.getAllDebaters().thenAccept(debaterModels -> {
            List<Debater> debaters = new ArrayList<>();

            // Получаем ID всех дебатов из моделей дебатеров
            List<Long> debatesIds = debaterModels.stream()
                    .flatMap(debater -> debater.getDebatesIds().stream())
                    .distinct()
                    .collect(Collectors.toList());

            // Используем getDebates для получения дебатов по их ID
            getDebates(debatesIds).thenAccept(debates -> {
                // Создаем карту для быстрого доступа к дебатам по их ID
                Map<Long, Debate> debatesMap = debates.stream()
                        .collect(Collectors.toMap(Debate::getId, Function.identity()));

                // Собираем информацию для каждого Debater
                for (DebaterModel debaterModel : debaterModels) {
                    Debater debater = new Debater();
                    debater.setMemberId(debaterModel.getMemberId());
                    debater.setNickname(debaterModel.getNickname());
                    debater.setWinnCount(debaterModel.getWinnDebatesCount());
                    debater.setLossesCount(debaterModel.getLossesDebatesCount());

                    List<Debate> debaterDebates = debaterModel.getDebatesIds().stream()
                            .map(debatesMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    debater.setDebates(debaterDebates);
                    debaters.add(debater);
                }

                // Завершаем CompletableFuture со списком debaters
                result.complete(debaters);
            }).exceptionally(e -> {
                result.completeExceptionally(e);
                return null;
            });
        }).exceptionally(e -> {
            result.completeExceptionally(e);
            return null;
        });

        return result;
    }

    public CompletableFuture<List<Debater>> getDebatersByMemberId(List<Long> memberIds) {
        CompletableFuture<List<Debater>> result = new CompletableFuture<>();

        dataBase.getDebatersByMemberIds(memberIds).thenAccept(debaterModels -> {
            List<Debater> debaters = debaterModels.stream().map(debaterModel -> {
                Debater debater = new Debater();
                debater.setMemberId(debaterModel.getMemberId());
                debater.setNickname(debaterModel.getNickname());
                debater.setWinnCount(debaterModel.getWinnDebatesCount());
                debater.setLossesCount(debaterModel.getLossesDebatesCount());

                // Преобразование ID дебатов в объекты Debate
                List<Long> debateIds = debaterModel.getDebatesIds();
//                getDebates(debateIds).thenAccept(debates -> {
//                    debater.setDebates(debates);
//                });

                return getDebates(debateIds).thenApply(debates -> {
                    debater.setDebates(debates);

                    return debater;
                }).join();

            }).collect(Collectors.toList());

            // Завершаем CompletableFuture после преобразования всех моделей
            result.complete(debaters);
        }).exceptionally(e -> {
            result.completeExceptionally(e);
            return null;
        });

        return result;
    }

    public CompletableFuture<Debater> getDebater(int memberId) {
        return dataBase.getDebater(memberId).thenApply(debaterModel -> {
            Debater debater = new Debater();
            debater.setMemberId(debaterModel.getMemberId());
            debater.setNickname(debaterModel.getNickname());
            debater.setWinnCount(debaterModel.getWinnDebatesCount());
            debater.setLossesCount(debaterModel.getLossesDebatesCount());
            return debater;
        });
    }

    public CompletableFuture<Boolean> addDebaters(List<Debater> debaters) {
        try {
            List<DebaterModel> debaterModels = debaters.stream()
                    .map(debater -> {
                        DebaterModel debaterModel = new DebaterModel();
                        debaterModel.setMemberId(debater.getMemberId());
                        debaterModel.setNickname(debater.getNickname());
                        debaterModel.setServerNickname(debater.getServerNickname());
                        debaterModel.setWinnDebatesCount(debater.getWinnCount());
                        debaterModel.setLossesDebatesCount(debater.getLossesCount());
                        debaterModel.setDebatesIds(debater.getDebates().stream()
                                .map(Debate::getId)
                                .collect(Collectors.toList()));
                        return debaterModel;
                    })
                    .collect(Collectors.toList());
            return dataBase.addDebaters(debaterModels);
        } catch (Exception e) {
            e.printStackTrace();
            return CompletableFuture.completedFuture(false);
        }
    }

    public CompletableFuture<List<Debate>> getDebates(List<Long> debateIds) {
        CompletableFuture<List<Debate>> result = new CompletableFuture<>();

        dataBase.getDebates(debateIds).thenAccept(debateModels -> {
            List<Debate> debates = new ArrayList<>();
            List<Long> allGovernmentMembersIds = new ArrayList<>();
            List<Long> allOppositionMembersIds = new ArrayList<>();
            List<Integer> allThemesIds = new ArrayList<>();

            for (DebateModel debateModel : debateModels) {
                allGovernmentMembersIds.addAll(debateModel.getGovernmentMembersIds());
                allOppositionMembersIds.addAll(debateModel.getOppositionMembersIds());
                allThemesIds.add(debateModel.getThemeId());
            }

            CompletableFuture<List<Member>> governmentMembersFuture = apiService.getMembersByIds(allGovernmentMembersIds);
            CompletableFuture<List<Member>> oppositionMembersFuture = apiService.getMembersByIds(allOppositionMembersIds);
            CompletableFuture<List<ThemeModel>> themesFuture = dataBase.getThemes(allThemesIds);
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(governmentMembersFuture, oppositionMembersFuture);

            allFutures.thenAccept((v) -> {
                try {
                    List<Member> allGovernmentMembers = governmentMembersFuture.get();
                    List<Member> allOppositionMembers = oppositionMembersFuture.get();
                    List<ThemeModel> allThemes = themesFuture.get();

                    for (DebateModel debateModel : debateModels) {
                        Debate debate = new Debate();

                        List<Member> governmentMembers = new ArrayList<>();
                        for (long memberId : debateModel.getGovernmentMembersIds()) {
                            for (Member member : allGovernmentMembers) {
                                if (member.getIdLong() == memberId) {
                                    governmentMembers.add(member);
                                    break;
                                }
                            }
                        }

                        List<Member> oppositionMembers = new ArrayList<>();
                        for (long memberId : debateModel.getOppositionMembersIds()) {
                            for (Member member : allOppositionMembers) {
                                if (member.getIdLong() == memberId) {
                                    oppositionMembers.add(member);
                                    break;
                                }
                            }
                        }

                        ThemeModel themeModel = allThemes.stream()
                                .filter(theme -> theme.getId() == debateModel.getThemeId())
                                .findFirst()
                                .orElse(null);

                        Theme theme = new Theme();
                        theme.setId(themeModel != null ? themeModel.getId() : 0);
                        theme.setName(themeModel != null ? themeModel.getName() : "");
                        theme.setUsageCount(themeModel != null ? themeModel.getUsageCount() : 0);

                        debate.setId(debateModel.getId());
                        debate.setTheme(theme);
                        debate.setEndDateTime(debateModel.getStartDateTime());
                        debate.setIsGovernmentWinner(debateModel.isGovernmentWinner());
                        debate.setGovernmentDebaters(governmentMembers);
                        debate.setOppositionDebaters(oppositionMembers);

                        debates.add(debate);
                    }

                    result.complete(debates);
                } catch (InterruptedException | ExecutionException e) {
                    result.completeExceptionally(e);
                }
            }).exceptionally(e -> {
                result.completeExceptionally(e);
                return null;
            });
        }).exceptionally(e -> {
            result.completeExceptionally(e);
            return null;
        });

        return result;
    }

    public CompletableFuture<List<Debate>> getDebatesByMemberId(long memberId) {
        List<Debate> debates = new ArrayList<>();
        return dataBase.getDebatesByMemberId(memberId).thenCompose(debateModels -> {
            List<Long> allGovernmentMembersIds = new ArrayList<>();
            List<Long> allOppositionMembersIds = new ArrayList<>();
            List<Integer> allThemesIds = new ArrayList<>();

            for (DebateModel debateModel : debateModels) {
                allGovernmentMembersIds.addAll(debateModel.getGovernmentMembersIds());
                allOppositionMembersIds.addAll(debateModel.getOppositionMembersIds());
                allThemesIds.add(debateModel.getThemeId());
            }

            CompletableFuture<List<Member>> governmentMembersFuture = apiService.getMembersByIds(allGovernmentMembersIds);
            CompletableFuture<List<Member>> oppositionMembersFuture = apiService.getMembersByIds(allOppositionMembersIds);
            CompletableFuture<List<ThemeModel>> themesFuture = dataBase.getThemes(allThemesIds);
            return CompletableFuture.allOf(governmentMembersFuture, oppositionMembersFuture).thenApply(v -> {
                try {
                    List<Member> allGovernmentMembers = governmentMembersFuture.get();
                    List<Member> allOppositionMembers = oppositionMembersFuture.get();
                    List<ThemeModel> allThemes = themesFuture.get();

                    for (DebateModel debateModel : debateModels) {
                        Debate debate = new Debate();

                        List<Member> governmentMembers = new ArrayList<>();
                        for (long memberId1 : debateModel.getGovernmentMembersIds()) {
                            for (Member member : allGovernmentMembers) {
                                if (member.getIdLong() == memberId1) {
                                    governmentMembers.add(member);
                                    break;
                                }
                            }
                        }

                        List<Member> oppositionMembers = new ArrayList<>();
                        for (long memberId1 : debateModel.getOppositionMembersIds()) {
                            for (Member member : allOppositionMembers) {
                                if (member.getIdLong() == memberId1) {
                                    oppositionMembers.add(member);
                                    break;
                                }
                            }
                        }

                        ThemeModel themeModel = allThemes.stream()
                                .filter(theme -> theme.getId() == debateModel.getThemeId())
                                .findFirst()
                                .orElse(null);

                        Theme theme = new Theme();
                        theme.setId(themeModel != null ? themeModel.getId() : 0);
                        theme.setName(themeModel != null ? themeModel.getName() : "");
                        theme.setUsageCount(themeModel != null ? themeModel.getUsageCount() : 0);

                        debate.setId(debateModel.getId());
                        debate.setTheme(theme);
                        debate.setEndDateTime(debateModel.getStartDateTime());
                        debate.setIsGovernmentWinner(debateModel.isGovernmentWinner());
                        debate.setGovernmentDebaters(governmentMembers);
                        debate.setOppositionDebaters(oppositionMembers);

                        debates.add(debate);
                    }

                    return debates;
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }).exceptionally(th -> {
            throw new RuntimeException(th);
        });
    }

    public CompletableFuture<List<Debate>> getDebatesByMemberIds(List<Long> memberIds) {
        List<Debate> debates = new ArrayList<>();
        return dataBase.getDebatesByMemberId(memberIds).thenCompose(debateModels -> {
            List<Long> allGovernmentMembersIds = new ArrayList<>();
            List<Long> allOppositionMembersIds = new ArrayList<>();
            List<Integer> allThemesIds = new ArrayList<>();

            for (DebateModel debateModel : debateModels) {
                allGovernmentMembersIds.addAll(debateModel.getGovernmentMembersIds());
                allOppositionMembersIds.addAll(debateModel.getOppositionMembersIds());
                allThemesIds.add(debateModel.getThemeId());
            }

            CompletableFuture<List<Member>> governmentMembersFuture = apiService.getMembersByIds(allGovernmentMembersIds);
            CompletableFuture<List<Member>> oppositionMembersFuture = apiService.getMembersByIds(allOppositionMembersIds);
            CompletableFuture<List<ThemeModel>> themesFuture = dataBase.getThemes(allThemesIds);
            return CompletableFuture.allOf(governmentMembersFuture, oppositionMembersFuture).thenApply(v -> {
                try {
                    List<Member> allGovernmentMembers = governmentMembersFuture.get();
                    List<Member> allOppositionMembers = oppositionMembersFuture.get();
                    List<ThemeModel> allThemes = themesFuture.get();

                    for (DebateModel debateModel : debateModels) {
                        Debate debate = new Debate();

                        List<Member> governmentMembers = new ArrayList<>();
                        for (long memberId1 : debateModel.getGovernmentMembersIds()) {
                            for (Member member : allGovernmentMembers) {
                                if (member.getIdLong() == memberId1) {
                                    governmentMembers.add(member);
                                    break;
                                }
                            }
                        }

                        List<Member> oppositionMembers = new ArrayList<>();
                        for (long memberId1 : debateModel.getOppositionMembersIds()) {
                            for (Member member : allOppositionMembers) {
                                if (member.getIdLong() == memberId1) {
                                    oppositionMembers.add(member);
                                    break;
                                }
                            }
                        }

                        ThemeModel themeModel = allThemes.stream()
                                .filter(theme -> theme.getId() == debateModel.getThemeId())
                                .findFirst()
                                .orElse(null);

                        Theme theme = new Theme();
                        theme.setId(themeModel != null ? themeModel.getId() : 0);
                        theme.setName(themeModel != null ? themeModel.getName() : "");
                        theme.setUsageCount(themeModel != null ? themeModel.getUsageCount() : 0);

                        debate.setId(debateModel.getId());
                        debate.setTheme(theme);
                        debate.setEndDateTime(debateModel.getStartDateTime());
                        debate.setIsGovernmentWinner(debateModel.isGovernmentWinner());
                        debate.setGovernmentDebaters(governmentMembers);
                        debate.setOppositionDebaters(oppositionMembers);

                        debates.add(debate);
                    }

                    return debates;
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }).exceptionally(th -> {
            throw new RuntimeException(th);
        });
    }

    public CompletableFuture<Debate> getDebate(long debateId) {
        return dataBase.getDebate(debateId).thenCompose(debateModel -> {
            Debate debate = new Debate();
            debate.setId(debateModel.getId());
            debate.setEndDateTime(debateModel.getStartDateTime());
            debate.setIsGovernmentWinner(debateModel.isGovernmentWinner());

            List<Long> allGovernmentMembersIds = debateModel.getGovernmentMembersIds();
            List<Long> allOppositionMembersIds = debateModel.getOppositionMembersIds();
            int themeId = debateModel.getThemeId();

            CompletableFuture<List<Member>> governmentMembersFuture = apiService.getMembersByIds(allGovernmentMembersIds);
            CompletableFuture<List<Member>> oppositionMembersFuture = apiService.getMembersByIds(allOppositionMembersIds);
            CompletableFuture<ThemeModel> themeFuture = dataBase.getTheme(themeId);
            return CompletableFuture.allOf(governmentMembersFuture, oppositionMembersFuture).thenApply(v -> {
                try {
                    List<Member> governmentMembers = governmentMembersFuture.get();
                    List<Member> oppositionMembers = oppositionMembersFuture.get();
                    ThemeModel themeModel = themeFuture.get();

                    List<Member> governmentMembersList = new ArrayList<>();
                    for (long memberId : allGovernmentMembersIds) {
                        for (Member member : governmentMembers) {
                            if (member.getIdLong() == memberId) {
                                governmentMembersList.add(member);
                                break;
                            }
                        }
                    }

                    List<Member> oppositionMembersList = new ArrayList<>();
                    for (long memberId : allOppositionMembersIds) {
                        for (Member member : oppositionMembers) {
                            if (member.getIdLong() == memberId) {
                                oppositionMembersList.add(member);
                                break;
                            }
                        }
                    }

                    Theme theme = new Theme();
                    theme.setId(themeModel.getId());
                    theme.setName(themeModel.getName());
                    theme.setUsageCount(themeModel.getUsageCount());

                    debate.setTheme(theme);
                    debate.setGovernmentDebaters(governmentMembersList);
                    debate.setOppositionDebaters(oppositionMembersList);

                    return debate;
                } catch (InterruptedException | ExecutionException e) {
                    // Обрабатываем исключения, возникающие в процессе выполнения CompletableFuture
                    throw new RuntimeException(e);
                }
            });
        }).exceptionally(th -> {
            // Обрабатываем исключения, возникающие на любом этапе асинхронной цепочки
            throw new RuntimeException(th);
        });
    }

    public CompletableFuture<Debate> getLastDebate() {
        return dataBase.getLastDebate().thenCompose(debateModel -> {
            Debate debate = new Debate();
            debate.setId(debateModel.getId());
            debate.setEndDateTime(debateModel.getStartDateTime());
            debate.setIsGovernmentWinner(debateModel.isGovernmentWinner());

            List<Long> allGovernmentMembersIds = debateModel.getGovernmentMembersIds();
            List<Long> allOppositionMembersIds = debateModel.getOppositionMembersIds();
            int themeId = debateModel.getThemeId();

            CompletableFuture<List<Member>> governmentMembersFuture = apiService.getMembersByIds(allGovernmentMembersIds);
            CompletableFuture<List<Member>> oppositionMembersFuture = apiService.getMembersByIds(allOppositionMembersIds);
            CompletableFuture<ThemeModel> themeFuture = dataBase.getTheme(themeId);
            return CompletableFuture.allOf(governmentMembersFuture, oppositionMembersFuture).thenApply(v -> {
                try {
                    List<Member> governmentMembers = governmentMembersFuture.get();
                    List<Member> oppositionMembers = oppositionMembersFuture.get();
                    ThemeModel themeModel = themeFuture.get();

                    List<Member> governmentMembersList = new ArrayList<>();
                    for (long memberId : allGovernmentMembersIds) {
                        for (Member member : governmentMembers) {
                            if (member.getIdLong() == memberId) {
                                governmentMembersList.add(member);
                                break;
                            }
                        }
                    }

                    List<Member> oppositionMembersList = new ArrayList<>();
                    for (long memberId : allOppositionMembersIds) {
                        for (Member member : oppositionMembers) {
                            if (member.getIdLong() == memberId) {
                                oppositionMembersList.add(member);
                                break;
                            }
                        }
                    }

                    Theme theme = new Theme();
                    theme.setId(themeModel.getId());
                    theme.setName(themeModel.getName());
                    theme.setUsageCount(themeModel.getUsageCount());

                    debate.setTheme(theme);
                    debate.setGovernmentDebaters(governmentMembersList);
                    debate.setOppositionDebaters(oppositionMembersList);

                    return debate;
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        }).exceptionally(th -> {
            throw new RuntimeException(th);
        });
    }

    public CompletableFuture<Debate> addDebate(Debate debate) {
        DebateModel debateModel = new DebateModel();
        debateModel.setId(debate.getId());
        debateModel.setThemeId(debate.getTheme().getId());
        debateModel.setGovernmentMembersIds(debate.getGovernmentDebaters().stream()
                .map(Member::getIdLong)
                .collect(Collectors.toList()));
        debateModel.setOppositionMembersIds(debate.getOppositionDebaters().stream()
                .map(Member::getIdLong)
                .collect(Collectors.toList()));
        debateModel.setStartDateTime(debate.getEndDateTime());
        debateModel.setGovernmentWinner(debate.isGovernmentWinner());

        return dataBase.addDebate(debateModel).thenApply(resultDebateModel -> {
            debate.setId(resultDebateModel.getId());
            return debate;
        });
    }

    public CompletableFuture<List<Question>> getQuestions(List<Integer> questionIds) {
        return dataBase.getQuestions(questionIds).thenApply(questionModels -> questionModels.stream()
                .map(questionModel -> {
                    Question question = new Question();
                    question.setId(questionModel.getId());
                    question.setText(questionModel.getText());
                    question.setAnswers(questionModel.getAnswers());
                    question.setCorrectAnswer(questionModel.getCorrectAnswer());
                    return question;
                })
                .collect(Collectors.toList()));
    }

    public CompletableFuture<List<Question>> getAllQuestions() {
        return dataBase.getAllQuestions().thenApply(questionModels -> questionModels.stream()
                .map(questionModel -> {
                    Question question = new Question();
                    question.setId(questionModel.getId());
                    question.setText(questionModel.getText());
                    question.setAnswers(questionModel.getAnswers());
                    question.setCorrectAnswer(questionModel.getCorrectAnswer());
                    return question;
                })
                .collect(Collectors.toList()));
    }

}