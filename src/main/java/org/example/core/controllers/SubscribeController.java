package org.example.core.controllers;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import org.example.core.models.Question;
import org.example.core.models.TestDataByUser;
import org.example.resources.StringRes;
import org.example.core.constants.CategoriesID;
import org.example.core.constants.RolesID;
import org.example.core.constants.TextChannelsID;
import org.example.domain.UseCase;
import org.example.core.constants.VoiceChannelsID;
import org.jetbrains.annotations.NotNull;

public class SubscribeController {
    private static final int DEBATERS_LIMIT = 1; //4
    private static final int JUDGES_LIMIT = 1; //1
    private static final int START_DEBATE_TIMER = 5; //60

    private static final String DEBATER_SUBSCRIBE_BTN_ID = "debater_subscribe";
    private static final String JUDGE_SUBSCRIBE_BTN_ID = "judge_subscribe";
    private static final String UNSUBSCRIBE_BTN_ID = "unsubscribe";
    private static final String START_TEST_ID = "start_test";
    private static final String ANSWER_A_ID = "answer_a";
    private static final String ANSWER_B_ID = "answer_b";
    private static final String ANSWER_C_ID = "answer_c";
    private static final String ANSWER_D_ID = "answer_d";
    private static final String CLOSE_TEST_ID = "close_test";

    private static final int MAX_QUESTIONS = 10;


    private final RatingController ratingController;
    private final HistoryController historyController;

    private final TextChannel channel;
    private final UseCase useCase;

    private final List<Long> debaterRoles;
    private final List<String> voiceChannelsNames;
    private final List<Member> subscribeDebatersList = new ArrayList<>();
    private final List<Member> subscribeJudgesList = new ArrayList<>();
    private final Map<String, Integer> answerButtonIdByAnswersIndex = Map.of(
            ANSWER_A_ID, 0,
            ANSWER_B_ID, 1,
            ANSWER_C_ID, 2,
            ANSWER_D_ID, 3
    );

    private LinkedHashMap<Member, TestDataByUser> testDataByUserMap = new LinkedHashMap<>();

    private long timerForStartDebate = 0;
    private boolean isDebateStarted = false;
    public DebateController debateController;
    private ScheduledFuture<?> debateStartTask;

    public SubscribeController(UseCase useCase) {
        this.useCase = useCase;
        this.channel = useCase.getTextChannel(TextChannelsID.SUBSCRIBE);
        this.ratingController = RatingController.getInstance(useCase);
        this.historyController = HistoryController.getInstance(useCase);

        debaterRoles = List.of(
                RolesID.HEAD_GOVERNMENT,
                RolesID.HEAD_OPPOSITION,
                RolesID.MEMBER_GOVERNMENT,
                RolesID.MEMBER_OPPOSITION
        );

        voiceChannelsNames = List.of(
                StringRes.CHANNEL_JUDGE,
                StringRes.CHANNEL_TRIBUNE,
                StringRes.CHANNEL_GOVERNMENT,
                StringRes.CHANNEL_OPPOSITION
        );

        showSubscribeMessage();
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        switch (event.getComponentId()) {
            case DEBATER_SUBSCRIBE_BTN_ID -> onClickDebaterSubscribeBtn(event, member);
            case JUDGE_SUBSCRIBE_BTN_ID -> onClickJudgeSubscribeBtn(event, member);
            case UNSUBSCRIBE_BTN_ID -> onClickUnsubscribe(event, member);
            case START_TEST_ID -> onClickStartTest(event, member);
            case ANSWER_A_ID, ANSWER_B_ID, ANSWER_C_ID, ANSWER_D_ID -> onClickAnswer(event, member);
            case CLOSE_TEST_ID -> onClickCloseTest(event, member);
        }
    }

    public void onLeaveFromVoiceChannel(@NotNull GuildVoiceUpdateEvent event) {
        boolean isDebaterSubscriber = subscribeDebatersList.stream().anyMatch(user -> user.getId().equals(event.getMember().getId()));
        boolean isJudgeSubscriber = subscribeJudgesList.stream().anyMatch(user -> user.getId().equals(event.getMember().getId()));
        if (timerForStartDebate == 0) {
            if (!isDebateStarted) {
                if (isDebaterSubscriber) removeDebaterFromList(event.getMember(), null);
                if (isJudgeSubscriber) removeJudgeFromList(event.getMember(), null);
            }
        } else {
            if (isDebaterSubscriber) removeDebaterFromList(event.getMember(), null);
            if (isJudgeSubscriber) removeJudgeFromList(event.getMember(), null);
        }
    }

    private void showSubscribeMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(new Color(88, 100, 242));
        embedBuilder.setTitle(StringRes.TITLE_DEBATE_SUBSCRIBE);
        embedBuilder.addField(StringRes.TITLE_DEBATER_LIST, StringRes.DESCRIPTION_NO_MEMBERS, true);
        embedBuilder.addField(StringRes.TITLE_JUDGES_LIST, StringRes.DESCRIPTION_NO_MEMBERS, true);

        Button debaterButton = Button.primary(DEBATER_SUBSCRIBE_BTN_ID, StringRes.BUTTON_SUBSCRIBE_DEBATER);
        Button judgeButton = Button.primary(JUDGE_SUBSCRIBE_BTN_ID, StringRes.BUTTON_SUBSCRIBE_JUDGE);
        Button unsubscribeButton = Button.danger(UNSUBSCRIBE_BTN_ID, StringRes.BUTTON_UNSUBSCRIBE);

        channel.getHistoryFromBeginning(1).queue(history -> {
            if (!history.getRetrievedHistory().isEmpty()) {
                Message existingMessage = history.getRetrievedHistory().get(0);
                channel.editMessageEmbedsById(existingMessage.getId(), embedBuilder.build())
                        .setActionRow(debaterButton, judgeButton, unsubscribeButton)
                        .queue();
            } else {
                channel.sendMessageEmbeds(embedBuilder.build())
                        .setActionRow(debaterButton, judgeButton, unsubscribeButton)
                        .queue();
            }
        });
    }

    private void onClickDebaterSubscribeBtn(@NotNull ButtonInteractionEvent event, Member member) {
        if (event.getMember() == null) return;

        if (isMemberHasDebaterRole(event.getMember())) {
            useCase.showEphemeralLoading(event).thenAccept(message -> {
                AudioChannelUnion voiceChannel = Objects.requireNonNull(event.getMember().getVoiceState()).getChannel();

                if (voiceChannel == null) {
                    message.editOriginal(StringRes.WARNING_NEED_WAITING_ROOM).queue();
                    return;
                }

                if (isMemberNotInWaitingRoom(voiceChannel)) {
                    message.editOriginal(StringRes.WARNING_NEED_WAITING_ROOM).queue();
                    return;
                }

                if (subscribeDebatersList.contains(member)) {
                    message.editOriginal(StringRes.WARNING_ALREADY_DEBATER).queue();
                    return;
                }

                addDebaterToList(member, () -> message.editOriginal(StringRes.REMARK_DEBATER_ADDED).queue());
            });
        } else {
            useCase.showEphemeral(event).thenAccept(this::showNeedToStartTestEmbed);
        }
    }

    private void onClickJudgeSubscribeBtn(@NotNull ButtonInteractionEvent event, Member member) {
        useCase.showEphemeralLoading(event).thenAccept(message -> {
            if (event.getMember() == null) return;

            AudioChannelUnion voiceChannel = Objects.requireNonNull(event.getMember().getVoiceState()).getChannel();

            if (voiceChannel == null || isMemberNotInWaitingRoom(voiceChannel)) {
                message.editOriginal(StringRes.WARNING_NEED_WAITING_ROOM).queue();
                return;
            }

            if (!isMemberHasJudgeRole(event.getMember())) {
                message.editOriginal(StringRes.WARNING_NEED_JUDGE_ROLE).queue();
                return;
            }

            if (subscribeJudgesList.contains(member)) {
                message.editOriginal(StringRes.WARNING_ALREADY_JUDGE).queue();
                return;
            }

            addJudgeToList(member, () -> message.editOriginal(StringRes.REMARK_JUDGE_ADDED).queue());
        });
    }

    private void onClickUnsubscribe(ButtonInteractionEvent event, Member member) {
        useCase.showEphemeralLoading(event).thenAccept(message -> {
            channel.getHistoryFromBeginning(1).queue(history -> {
                if (history.isEmpty()) {
                    message.editOriginal(StringRes.WARNING_NEED_SUBSCRIBED).queue();
                    return;
                }

                MessageEmbed embed = history.getRetrievedHistory().get(0).getEmbeds().get(0);
                String debatersList = embed.getFields().get(0).getValue();
                String judgeList = embed.getFields().get(1).getValue();

                if (isMemberInList(debatersList, member)) {
                    removeDebaterFromList(member, () -> message.editOriginal(StringRes.REMARK_DEBATER_REMOVED).queue());
                } else if (isMemberInList(judgeList, member)) {
                    removeJudgeFromList(member, () -> message.editOriginal(StringRes.REMARK_JUDGE_REMOVED).queue());
                } else {
                    message.editOriginal(StringRes.WARNING_NEED_SUBSCRIBED).queue();
                }
            });
        });
    }

    private void onClickStartTest(ButtonInteractionEvent event, Member member) {
        useCase.getAllQuestions().thenAccept(questions -> {
            TestDataByUser testDataByUser = new TestDataByUser(member, questions);
            if (testDataByUserMap.size() > 20) {
                removeFirstEntry(testDataByUserMap);
            }
            testDataByUserMap.put(member, testDataByUser);
            showQuestion(event, 1);
        });
    }

    private void onClickAnswer(ButtonInteractionEvent event, Member member) {
        TestDataByUser currentTestData = testDataByUserMap.get(event.getMember());
        Question currentQuestion = currentTestData.getCurrentQuestion();

        if (currentTestData.getCurrentQuestionNumber() >= MAX_QUESTIONS) {
            showTestSuccess(event);
            return;
        }

        String selectedAnswer = currentQuestion.getAnswers().get(answerButtonIdByAnswersIndex.get(event.getComponentId()));
        boolean isSelectedAnswerCorrect = currentQuestion.getCorrectAnswer().equals(selectedAnswer);

        if (isSelectedAnswerCorrect) {
            showQuestion(event, currentTestData.getCurrentQuestionNumber() + 1);
        } else {
            showTestFailed(event);
        }
    }

    private void onClickCloseTest(ButtonInteractionEvent event, Member member) {
        event.deferEdit().queue();
        event.getHook().deleteOriginal().queue(
                success -> System.out.println("Эфемерное сообщение удалено"),
                failure -> System.err.println("Ошибка при удалении эфемерного сообщения: " + failure.getMessage())
        );
    }

    private void startDebateTimer(long messageId, EmbedBuilder embed) {
        timerForStartDebate = System.currentTimeMillis() / 1000L + START_DEBATE_TIMER;
        embed.addField(StringRes.TITLE_TIMER, "<t:" + timerForStartDebate + ":R>", false);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        debateStartTask = scheduler.schedule(() -> {
            if (System.currentTimeMillis() / 1000L >= timerForStartDebate) {
                channel.retrieveMessageById(messageId).queue(message -> {
                    updateDebateMessage(message);

                    timerForStartDebate = 0;
                    isDebateStarted = true;
                    debateController = new DebateController(useCase, this);
                    setupDebateRoles(subscribeDebatersList, subscribeJudgesList, () -> createVoiceChannels(voiceChannelsNames));
                });
            }
        }, START_DEBATE_TIMER, TimeUnit.SECONDS);
    }

    private void updateDebateMessage(Message message) {
        EmbedBuilder embedBuilder = new EmbedBuilder(message.getEmbeds().get(0));
        embedBuilder.clearFields()
                .addField(StringRes.TITLE_DEBATER_LIST, StringRes.DESCRIPTION_NO_MEMBERS, true)
                .addField(StringRes.TITLE_JUDGES_LIST, StringRes.DESCRIPTION_NO_MEMBERS, true)
                .addField(StringRes.TITLE_TIMER, StringRes.DESCRIPTION_NEED_GO_TO_TRIBUNE, false);

        channel.editMessageEmbedsById(message.getId(), embedBuilder.build())
                .setActionRow(
                        Button.primary(DEBATER_SUBSCRIBE_BTN_ID, StringRes.BUTTON_SUBSCRIBE_DEBATER).asDisabled(),
                        Button.primary(JUDGE_SUBSCRIBE_BTN_ID, StringRes.BUTTON_SUBSCRIBE_JUDGE).asDisabled(),
                        Button.danger(UNSUBSCRIBE_BTN_ID, StringRes.BUTTON_UNSUBSCRIBE).asDisabled()
                ).queue();
    }

    private void createVoiceChannels(List<String> channelNames) {
        Category category = useCase.getCategoryByID(CategoriesID.DEBATE_CATEGORY).join();
        if (category != null) {
            Guild guild = category.getGuild();
            Role everyoneRole = guild.getPublicRole();
            List<VoiceChannel> existingChannels = category.getVoiceChannels();

            for (String channelName : channelNames) {
                boolean isJudge = channelName.equals(StringRes.CHANNEL_JUDGE);
                boolean isTribune = channelName.equals(StringRes.CHANNEL_TRIBUNE);
                boolean isGovernment = channelName.equals(StringRes.CHANNEL_GOVERNMENT);
                boolean isOpposition = channelName.equals(StringRes.CHANNEL_OPPOSITION);

                HashMap<Long, List<Permission>> allowPermissions = new HashMap<>();
                HashMap<Long, List<Permission>> denyPermissions = new HashMap<>();

                for (VoiceChannel existingChannel : existingChannels) {
                    if (existingChannel.getName().equalsIgnoreCase(channelName)) {
                        existingChannel.delete().queue();
                        break;
                    }
                }

                if (isJudge) {
                    allowPermissions.put(RolesID.JUDGE, List.of(Permission.VOICE_CONNECT));
                    denyPermissions.put(everyoneRole.getIdLong(), List.of(Permission.VOICE_CONNECT));
                } else if (isTribune) {
                    allowPermissions.put(everyoneRole.getIdLong(), List.of(Permission.VOICE_CONNECT));
                } else if (isGovernment) {
                    allowPermissions.put(RolesID.HEAD_GOVERNMENT, List.of(Permission.VOICE_CONNECT));
                    allowPermissions.put(RolesID.MEMBER_GOVERNMENT, List.of(Permission.VOICE_CONNECT));
                    denyPermissions.put(everyoneRole.getIdLong(), List.of(Permission.VOICE_CONNECT));
                } else if (isOpposition) {
                    allowPermissions.put(RolesID.HEAD_OPPOSITION, List.of(Permission.VOICE_CONNECT));
                    allowPermissions.put(RolesID.MEMBER_OPPOSITION, List.of(Permission.VOICE_CONNECT));
                    denyPermissions.put(everyoneRole.getIdLong(), List.of(Permission.VOICE_CONNECT));
                }

                category.createVoiceChannel(channelName)
                        .addRolePermissionOverride(everyoneRole.getIdLong(), allowPermissions.get(everyoneRole.getIdLong()), denyPermissions.get(everyoneRole.getIdLong()))
                        .addRolePermissionOverride(RolesID.JUDGE, allowPermissions.get(RolesID.JUDGE), denyPermissions.get(RolesID.JUDGE))
                        .addRolePermissionOverride(RolesID.HEAD_GOVERNMENT, allowPermissions.get(RolesID.HEAD_GOVERNMENT), denyPermissions.get(RolesID.HEAD_GOVERNMENT))
                        .addRolePermissionOverride(RolesID.HEAD_OPPOSITION, allowPermissions.get(RolesID.HEAD_OPPOSITION), denyPermissions.get(RolesID.HEAD_OPPOSITION))
                        .addRolePermissionOverride(RolesID.MEMBER_GOVERNMENT, allowPermissions.get(RolesID.MEMBER_GOVERNMENT), denyPermissions.get(RolesID.MEMBER_GOVERNMENT))
                        .addRolePermissionOverride(RolesID.MEMBER_OPPOSITION, allowPermissions.get(RolesID.MEMBER_OPPOSITION), denyPermissions.get(RolesID.MEMBER_OPPOSITION))
                        .queue(channel -> {
                            debateController.addChannel(channel);
                            if (isTribune) moveBotToVoiceChannel(channel);
                        });
            }
        } else {
//            Utils.sendLogError(useCase, "createVoiceChannels", "Категория не найдена. Проверьте ID.");
        }
    }

    private void setupDebateRoles(List<Member> subscribeDebatersList, List<Member> judgesList, Runnable callback) {
        Collections.shuffle(subscribeDebatersList);
        Map<Member, Long> membersToRolesMap = new HashMap<>();
        Map<Member, Long> judgesToRolesMap = new HashMap<>();

        for (int i = 0; i < DEBATERS_LIMIT; i++) {
            Member member = subscribeDebatersList.get(i);
            boolean hasDebaterRole = member.getRoles().stream().anyMatch(role -> debaterRoles.contains(role.getIdLong()));
            if (!hasDebaterRole) membersToRolesMap.put(member, debaterRoles.get(i));
        }

        for (int i = 0; i < JUDGES_LIMIT; i++) judgesToRolesMap.put(judgesList.get(i), RolesID.JUDGE);

        useCase.addRolesToMembers(membersToRolesMap).thenAccept(success -> {
            useCase.addRolesToMembers(judgesToRolesMap).thenAccept(success1 -> {
                if (callback != null && success && success1) callback.run();
            });
        });
    }

    private void moveBotToVoiceChannel(VoiceChannel channel) {
        if (channel != null) {
            Guild guild = channel.getGuild();
            AudioManager audioManager = guild.getAudioManager();
            if (!audioManager.isConnected()) {
                audioManager.openAudioConnection(channel);
//                Utils.sendLogDebug(useCase, "joinVoiceChannel", "Бот подключен к голосовому каналу: " +
//                channel.getName());
            } else {
//                Utils.sendLogError(useCase, "joinVoiceChannel", "Бот уже находится в голосовом канале или
//                пытается подключиться.");
            }
        } else {
//            Utils.sendLogError(useCase, "joinVoiceChannel", "Голосовой канал не найден.");
        }
    }

    private void updateList(Runnable callback) {
        channel.getHistoryFromBeginning(1).queue(history -> {
            if (!history.isEmpty()) {
                Message message = history.getRetrievedHistory().get(0);
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(new Color(88, 100, 242));
                embedBuilder.setTitle(StringRes.TITLE_DEBATE_SUBSCRIBE);

                List<String> debaters = subscribeDebatersList.stream().map(Member::getAsMention).toList();
                String debaterListString = debaters.isEmpty() ? StringRes.DESCRIPTION_NO_MEMBERS : String.join("\n", debaters);
                embedBuilder.addField(StringRes.TITLE_DEBATER_LIST, debaterListString, true);

                List<String> judges = subscribeJudgesList.stream().map(Member::getAsMention).toList();
                String judgeListString = judges.isEmpty() ? StringRes.DESCRIPTION_NO_MEMBERS : String.join("\n", judges);
                embedBuilder.addField(StringRes.TITLE_JUDGES_LIST, judgeListString, true);

                Button debaterButton = Button.primary(DEBATER_SUBSCRIBE_BTN_ID, StringRes.BUTTON_SUBSCRIBE_DEBATER);
                Button judgeButton = Button.primary(JUDGE_SUBSCRIBE_BTN_ID, StringRes.BUTTON_SUBSCRIBE_JUDGE);
                Button unsubscribeButton = Button.danger(UNSUBSCRIBE_BTN_ID, StringRes.BUTTON_UNSUBSCRIBE);

                if (subscribeDebatersList.size() >= DEBATERS_LIMIT && subscribeJudgesList.size() >= JUDGES_LIMIT && timerForStartDebate == 0) {
                    startDebateTimer(message.getIdLong(), embedBuilder);
                }

                channel.editMessageEmbedsById(message.getId(), embedBuilder.build()).setActionRow(debaterButton, judgeButton, unsubscribeButton).queue((msg) -> {
                    if (callback != null) callback.run();
                });
            }
        });
    }

    public void showNeedToStartTestEmbed(InteractionHook message) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(88, 100, 242));
        embed.setTitle("У вас нету роли дебатер.\nЧтобы получить роль нужно пройти тест на знание правил.");
        embed.setDescription("""
                - Чтобы подгоовиться к тесту прочитайте правила дебатов АПФ.\s
                - В тесте 20 вопросов, и 4 варианта ответа.\s
                - На каждый вопрос выделяется 30 секунд.
                - Тест автоматически закончится через 15 минут.
                """);

        Button startTestButton = Button.primary(START_TEST_ID, "Начать тест");

        message.editOriginalEmbeds(embed.build())
                .setActionRow(startTestButton)
                .queue(
                        success -> System.out.println("1Сообщение изменено"),
                        failure -> System.err.println("1Не удалось изменить оригинальное сообщение: ")
                );
    }

    public void showQuestion(ButtonInteractionEvent event, int questionNumber) {
        TestDataByUser currentTestData = testDataByUserMap.get(event.getMember());

        if (currentTestData.getQuestions().isEmpty()) {
            System.err.println("Список вопросов пуст");
            return;
        }

        Question currentQuestion = currentTestData.getQuestions().get(questionNumber - 1);

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(88, 100, 242));
        embed.setTitle(currentQuestion.getText());
        embed.setFooter((questionNumber) + " из " + MAX_QUESTIONS);

        String answersText = "**A**: " + currentQuestion.getAnswers().get(0) + "\n" +
                "**B**: " + currentQuestion.getAnswers().get(1) + "\n" +
                "**C**: " + currentQuestion.getAnswers().get(2) + "\n" +
                "**D**: " + currentQuestion.getAnswers().get(3);
        embed.setDescription(answersText);

        Button a = Button.primary(ANSWER_A_ID, "A");
        Button b = Button.primary(ANSWER_B_ID, "B");
        Button c = Button.primary(ANSWER_C_ID, "C");
        Button d = Button.primary(ANSWER_D_ID, "D");

        event.editMessageEmbeds(embed.build())
                .setActionRow(a, b, c, d)
                .queue(
                        success -> {
                            currentTestData.setCurrentQuestion(currentQuestion);
                            currentTestData.setCurrentQuestionNumber(questionNumber);
                            System.out.println("1Сообщение изменено");
                        },
                        failure -> System.err.println("1Не удалось изменить оригинальное сообщение: " + failure.getMessage())
                );


    }

    public void showTestSuccess(ButtonInteractionEvent event) {
        Map<Member, Long> members = Map.of(Objects.requireNonNull(event.getMember()), RolesID.DEBATER_APF);
        useCase.addRolesToMembers(members).thenAccept(success -> {
            EmbedBuilder winEmbed = new EmbedBuilder();
            winEmbed.setColor(new Color(36, 128, 70));
            winEmbed.setTitle("Тест пройден");
            winEmbed.setDescription("Вы правильно ответили на " + MAX_QUESTIONS + " вопросов.");

            event.editMessageEmbeds(winEmbed.build()).queue(
                    success1 -> System.out.println("Сообщение изменено"),
                    failure -> System.err.println("Не удалось изменить оригинальное сообщение: " + failure.getMessage()));
        });
    }

    public void showTestFailed(ButtonInteractionEvent event) {
        TestDataByUser currentTestData = testDataByUserMap.get(event.getMember());
        EmbedBuilder lossEmbed = new EmbedBuilder();
        lossEmbed.setColor(new Color(158, 26, 26));
        lossEmbed.setTitle("Тест провален :cry:");
        lossEmbed.setDescription("- Вы правильно ответили на " + (currentTestData.getCurrentQuestionNumber()) + " из " + MAX_QUESTIONS + " вопросов.\n" +
                "- Попробуйте еще раз через 10 минут.");

        event.editMessageEmbeds(lossEmbed.build()).setActionRow(Button.danger(CLOSE_TEST_ID, "Закрыть")).queue(
                success -> System.out.println("2Сообщение изменено"),
                failure -> System.err.println("2Не удалось изменить оригинальное сообщение: " + failure.getMessage()));
    }


    private void addDebaterToList(Member member, Runnable callback) {
        subscribeDebatersList.add(member);
        subscribeJudgesList.remove(member);
        updateList(callback);
    }

    private void addJudgeToList(Member member, Runnable callback) {
        subscribeJudgesList.add(member);
        subscribeDebatersList.remove(member);
        updateList(callback);
    }

    private void removeDebaterFromList(Member member, Runnable callback) {
        subscribeDebatersList.remove(member);
        if (timerForStartDebate != 0 && subscribeDebatersList.size() < DEBATERS_LIMIT) {
            cancelDebateStart(callback);
        } else {
            updateList(callback);
        }
    }

    private void removeJudgeFromList(Member member, Runnable callback) {
        subscribeJudgesList.remove(member);
        updateList(callback);
    }

    private void cancelDebateStart(Runnable callback) {
        if (debateStartTask != null && !debateStartTask.isDone()) {
            debateStartTask.cancel(true);
            debateStartTask = null;

            timerForStartDebate = 0;
            isDebateStarted = false;
            updateList(callback);
        }
    }

    public void endDebate() {
        if (debateStartTask != null && !debateStartTask.isDone()) {
            debateStartTask.cancel(true);
        }

        isDebateStarted = false;

        subscribeDebatersList.clear();
        subscribeJudgesList.clear();

        historyController.sendDebateTopicMessage();
        ratingController.displayDebatersList();
        showSubscribeMessage();
    }

    private boolean isMemberNotInWaitingRoom(AudioChannelUnion voiceChannel) {
        return voiceChannel.getIdLong() != VoiceChannelsID.WAITING_ROOM;
    }

    private boolean isMemberHasDebaterRole(Member member) {
        return member.getRoles().stream().anyMatch(role -> role.getIdLong() == RolesID.DEBATER_APF);
    }

    private boolean isMemberHasJudgeRole(Member member) {
        return member.getRoles().stream().anyMatch(role -> role.getIdLong() == RolesID.JUDGE_APF);
    }

    private boolean isMemberInList(String list, Member member) {
        return list != null && list.contains(member.getAsMention());
    }

    private static <K, V> void removeFirstEntry(LinkedHashMap<K, V> map) {
        // Получение итератора для ключей map
        Iterator<K> iterator = map.keySet().iterator();
        // Проверка, есть ли элемент для удаления
        if (iterator.hasNext()) {
            K firstKey = iterator.next(); // Получение первого ключа
            map.remove(firstKey); // Удаление первого элемента по ключу
        }
    }
}
