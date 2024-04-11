package org.example.core.controllers;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

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
    private static final int DEBATERS_LIMIT = 4; //4
    private static final int JUDGES_LIMIT = 1; //1
    private static final int START_DEBATE_TIMER = 10; //60
    private static final int TEST_TIMER = 20; //20
    private static final int TEST_ATTEMPTS = 1;

    private static final String DEBATER_SUBSCRIBE_BTN_ID = "debater_subscribe";
    private static final String JUDGE_SUBSCRIBE_BTN_ID = "judge_subscribe";
    private static final String UNSUBSCRIBE_BTN_ID = "unsubscribe";
    private static final String START_TEST_ID = "start_test";
    private static final String ANSWER_A_ID = "answer_a";
    private static final String ANSWER_B_ID = "answer_b";
    private static final String ANSWER_C_ID = "answer_c";
    private static final String ANSWER_D_ID = "answer_d";
    private static final String CLOSE_TEST_ID = "close_test";

    private static final int MAX_QUESTIONS = 3;

    private final TextChannel channel;
    private final UseCase useCase;
    private final RatingController ratingController;
    private final HistoryController historyController;

    public DebateController debateController;
    private ScheduledFuture<?> debateStartTask;

    private long timerForStartDebate = 0;
    private boolean isDebateStarted = false;

    private final List<Long> debaterRoles = new ArrayList<>(List.of(
            RolesID.HEAD_GOVERNMENT,
            RolesID.HEAD_OPPOSITION,
            RolesID.MEMBER_GOVERNMENT,
            RolesID.MEMBER_OPPOSITION
    ));

    private final List<String> voiceChannelsNames = new ArrayList<>(List.of(
            StringRes.CHANNEL_JUDGE,
            StringRes.CHANNEL_TRIBUNE,
            StringRes.CHANNEL_GOVERNMENT,
            StringRes.CHANNEL_OPPOSITION
    ));

    private List<Long> debatersRuleIds = new ArrayList<>(List.of(
            RolesID.DEBATER_APF_1,
            RolesID.DEBATER_APF_2
//            RolesID.DEBATER_APF_3,
//            RolesID.DEBATER_APF_4,
//            RolesID.DEBATER_APF_5
    ));


    private final List<Member> subscribeDebatersList = new ArrayList<>();
    private final List<Member> subscribeJudgesList = new ArrayList<>();
    private final LinkedHashMap<Member, TestDataByUser> testDataByUserMap = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Long> lastTestAttemptByUser = new LinkedHashMap<>();
    private final Map<Long, InteractionHook> memberByNeedToStartTestHook = new ConcurrentHashMap<>();

    private final Map<String, Integer> answerButtonIdByAnswersIndex = Map.of(
            ANSWER_A_ID, 0,
            ANSWER_B_ID, 1,
            ANSWER_C_ID, 2,
            ANSWER_D_ID, 3
    );

    public SubscribeController(UseCase useCase) {
        this.useCase = useCase;
        this.channel = useCase.getTextChannel(TextChannelsID.SUBSCRIBE);
        this.ratingController = RatingController.getInstance(useCase);
        this.historyController = HistoryController.getInstance(useCase);

//        debaterRoles = List.of(
//                RolesID.HEAD_GOVERNMENT,
//                RolesID.HEAD_OPPOSITION,
//                RolesID.MEMBER_GOVERNMENT,
//                RolesID.MEMBER_OPPOSITION
//        );
//
//        voiceChannelsNames = List.of(
//                StringRes.CHANNEL_JUDGE,
//                StringRes.CHANNEL_TRIBUNE,
//                StringRes.CHANNEL_GOVERNMENT,
//                StringRes.CHANNEL_OPPOSITION
//        );

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

        if (!isMemberHasDebaterRole(event.getMember())) {
            useCase.showEphemeral(event).thenAccept(hook -> {
                showNeedToStartTestEmbed(hook, member);
            });
            return;
        }

        useCase.showEphemeralShortLoading(event).thenAccept(message -> {
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

    }

    private void onClickJudgeSubscribeBtn(@NotNull ButtonInteractionEvent event, Member member) {
        if (event.getMember() == null) return;

        if (!isMemberHasDebaterRole(event.getMember())) {
            useCase.showEphemeral(event).thenAccept(hook -> {
                showNeedToStartTestEmbed(hook, member);
            });
            return;
        }
        useCase.showEphemeralShortLoading(event).thenAccept(message -> {
            AudioChannelUnion voiceChannel = Objects.requireNonNull(event.getMember().getVoiceState()).getChannel();

            if (voiceChannel == null || isMemberNotInWaitingRoom(voiceChannel)) {
                message.editOriginal(StringRes.WARNING_NEED_WAITING_ROOM).queue();
                return;
            }

//            if (!isMemberHasJudgeRole(event.getMember())) {
//                message.editOriginal(StringRes.WARNING_NEED_JUDGE_ROLE).queue();
//                return;
//            }

            if (subscribeJudgesList.contains(member)) {
                message.editOriginal(StringRes.WARNING_ALREADY_JUDGE).queue();
                return;
            }

            addJudgeToList(member, () -> message.editOriginal(StringRes.REMARK_JUDGE_ADDED).queue());
        });
    }

    private void onClickUnsubscribe(ButtonInteractionEvent event, Member member) {
        useCase.showEphemeralShortLoading(event).thenAccept(message -> {
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
        long currentTimeMillis = System.currentTimeMillis();
        boolean hasAttempt = lastTestAttemptByUser.containsKey(member.getIdLong());
        long timeSinceLastAttempt = hasAttempt ? currentTimeMillis - lastTestAttemptByUser.get(member.getIdLong()) : Long.MAX_VALUE;
        boolean isCoolDownPassed = timeSinceLastAttempt >= TimeUnit.MINUTES.toMillis(TEST_ATTEMPTS);
        long nextAttemptTime = hasAttempt ? (lastTestAttemptByUser.get(member.getIdLong()) + TimeUnit.MINUTES.toMillis(TEST_ATTEMPTS)) / 1000 : 0;

        if (hasAttempt && !isCoolDownPassed) {
            String textMessage = String.format("Вы можете начать тест заново только <t:%d:R> после последней попытки.", nextAttemptTime);
            useCase.showEphemeralShortLoading(event).thenAccept(hook -> {
                hook.editOriginal(textMessage).queue();
            });
            return;
        }

        if (testDataByUserMap.containsKey(event.getMember())) {
            useCase.showEphemeralShortLoading(event).thenAccept(message -> {
                message.editOriginal("Тест уже начат, подождите пока он закончится").queue();
            });
            return;
        }

        if (testDataByUserMap.size() > 20) {
            removeFirstEntry(testDataByUserMap);
        }

        if(lastTestAttemptByUser.size() > 20) {
            removeFirstEntry(lastTestAttemptByUser);
        }

        event.deferReply(true).queue(loadingHook -> {
            useCase.getAllQuestions().thenAccept(questions -> {
                loadingHook.deleteOriginal().queue();
                showFirstQuestion(member, questions);
            });
        });
    }

    private void onClickAnswer(ButtonInteractionEvent event, Member member) {
        TestDataByUser currentTestData = testDataByUserMap.get(event.getMember());
        ScheduledFuture<?> currentTimer = currentTestData.getTimers().remove(member);

        Question currentQuestion = currentTestData.getCurrentQuestion();
        String selectedAnswer = currentQuestion.getAnswers().get(answerButtonIdByAnswersIndex.get(event.getComponentId()));
        boolean isSelectedAnswerCorrect = currentQuestion.getCorrectAnswer().equals(selectedAnswer);

        if (isSelectedAnswerCorrect) {
            if (currentTimer != null) {
                currentTimer.cancel(false);
            }

            if (currentTestData.getCurrentQuestionNumber() == MAX_QUESTIONS) {
                showTestSuccess(event);
                return;
            }
            showNextQuestion(event, currentTestData.getCurrentQuestionNumber() + 1);
        } else {
            showTestFailed(event);
        }
    }

    private void onClickCloseTest(ButtonInteractionEvent event, Member member) {
        event.deferEdit().queue();
        event.getHook().deleteOriginal().queue(
                success -> System.out.println("Cообщение о закрытии теста удалено"),
                failure -> System.err.println("Не удалось удалить сообщение о закрытии теста: " + failure.getMessage())
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

        useCase.addRoleToMembers(membersToRolesMap).thenAccept(success -> {
            useCase.addRoleToMembers(judgesToRolesMap).thenAccept(success1 -> {
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
                System.out.println("Бот перемещен в канал " + channel.getName());
            } else {
                System.err.println("Бот уже находится в голосовом канале");
            }
        } else {
            System.err.println("Канал не найден");
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

                channel.editMessageEmbedsById(message.getId(), embedBuilder.build())
                        .setActionRow(debaterButton, judgeButton, unsubscribeButton)
                        .queue((msg) -> {
                    if (callback != null) callback.run();
                });
            }
        });
    }

    public void showNeedToStartTestEmbed(InteractionHook message, Member member) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(88, 100, 242));
        embed.setDescription("**:warning: У вас нету роли <@&" + RolesID.DEBATER_APF_1 + ">. Чтобы получить данную роль необходимо пройти тест на знание правил дебатов АПФ.**\n\n" +
                "- Чтобы подготовиться к тесту пройдите в канал <#" + TextChannelsID.RULES + ">.\n" +
                "- В тесте будут " + MAX_QUESTIONS + " вопросов и 4 варианта ответа.\n" +
                "- На каждый вопрос выделяется " + TEST_TIMER + " секунд.\n");

        Button startTestButton = Button.primary(START_TEST_ID, "Начать тест");

        memberByNeedToStartTestHook.put(member.getIdLong(), message);

        message.editOriginalEmbeds(embed.build())
                .setActionRow(startTestButton)
                .queue();
    }

    public void showNextQuestion(ButtonInteractionEvent event, int questionNumber) {
        TestDataByUser currentTestData = testDataByUserMap.get(event.getMember());

        if (currentTestData.getQuestions().isEmpty()) {
            showTestFailed(event);
            System.err.println("Список вопросов пуст");
            return;
        }

        ScheduledFuture<?> previousTimer = currentTestData.getTimers().remove(event.getMember());
        if (previousTimer != null) {
            previousTimer.cancel(false);
        }

        Question currentQuestion = currentTestData.getQuestions().get(questionNumber - 1);
        long currentTimeInSeconds = System.currentTimeMillis() / 1000L;
        long twentySecondsLater = currentTimeInSeconds + TEST_TIMER;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(88, 100, 242));
        embed.setFooter((questionNumber) + " из " + MAX_QUESTIONS);

        String timerText = ":stopwatch: <t:" + twentySecondsLater + ":R>\n\n";
        String questionText = "**" + currentQuestion.getText() + "**\n\n";
        String answersText = "**A**. " + currentQuestion.getAnswers().get(0) + "\n" +
                "**B**. " + currentQuestion.getAnswers().get(1) + "\n" +
                "**C**. " + currentQuestion.getAnswers().get(2) + "\n" +
                "**D**. " + currentQuestion.getAnswers().get(3) + "\n";

        embed.setDescription(timerText + questionText + answersText);

        Button a = Button.primary(ANSWER_A_ID, "A");
        Button b = Button.primary(ANSWER_B_ID, "B");
        Button c = Button.primary(ANSWER_C_ID, "C");
        Button d = Button.primary(ANSWER_D_ID, "D");

        event.editMessageEmbeds(embed.build())
                .setActionRow(a, b, c, d)
                .queue(message -> {
                            ScheduledFuture<?> timer = currentTestData
                                    .getScheduler()
                                    .schedule(() -> {
                                        showTestFailed(event.getMember(), message);
                                    }, TEST_TIMER, TimeUnit.SECONDS);

                            currentTestData.setCurrentQuestion(currentQuestion);
                            currentTestData.setCurrentQuestionNumber(questionNumber);
                            currentTestData.getTimers().put(event.getMember(), timer);

                            System.out.println("Сообщение о следующем вопросе изменено");
                        },
                        failure -> System.err.println("Не удалось изменить сообщение о следующем вопросе: " + failure.getMessage())
                );
    }

    public void showFirstQuestion(Member member, List<Question> questions) {
        TestDataByUser currentTestData = new TestDataByUser(member, questions);

        InteractionHook needToStartTestHook = memberByNeedToStartTestHook.get(member.getIdLong());
        memberByNeedToStartTestHook.remove(member.getIdLong());

        if (currentTestData.getQuestions().isEmpty()) {
            showTestFailed(member, needToStartTestHook);
            System.err.println("Список вопросов пуст");
            return;
        }

        ScheduledFuture<?> previousTimer = currentTestData.getTimers().remove(member);
        if (previousTimer != null) {
            previousTimer.cancel(false);
        }

        Question currentQuestion = currentTestData.getQuestions().get(0);
        long currentTimeInSeconds = System.currentTimeMillis() / 1000L;
        long twentySecondsLater = currentTimeInSeconds + TEST_TIMER;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(new Color(88, 100, 242));
        embed.setFooter((1) + " из " + MAX_QUESTIONS);

        String timerText = ":stopwatch: <t:" + twentySecondsLater + ":R>\n\n";
        String questionText = "**" + currentQuestion.getText() + "**\n\n";
        String answersText = "**A**. " + currentQuestion.getAnswers().get(0) + "\n" +
                "**B**. " + currentQuestion.getAnswers().get(1) + "\n" +
                "**C**. " + currentQuestion.getAnswers().get(2) + "\n" +
                "**D**. " + currentQuestion.getAnswers().get(3) + "\n";

        embed.setDescription(timerText + questionText + answersText);

        Button a = Button.primary(ANSWER_A_ID, "A");
        Button b = Button.primary(ANSWER_B_ID, "B");
        Button c = Button.primary(ANSWER_C_ID, "C");
        Button d = Button.primary(ANSWER_D_ID, "D");

        needToStartTestHook.editOriginalEmbeds(embed.build())
                .setActionRow(a, b, c, d)
                .queue(message -> {
                            ScheduledFuture<?> timer = currentTestData
                                    .getScheduler()
                                    .schedule(() -> {
                                        showTestFailed(member, message);
                                    }, TEST_TIMER, TimeUnit.SECONDS);

                            currentTestData.setCurrentQuestion(currentQuestion);
                            currentTestData.setCurrentQuestionNumber(1);
                            currentTestData.getTimers().put(member, timer);

                            testDataByUserMap.put(member, currentTestData);

                            System.out.println("Сообщение о первом вопросе изменено");
                        },
                        failure -> System.err.println("Не удалось изменить сообщение о первом вопросе: " + failure.getMessage())
                );
    }

    public void showTestSuccess(ButtonInteractionEvent event) {
        Map<Member, Long> members = Map.of(Objects.requireNonNull(event.getMember()), RolesID.DEBATER_APF_1);
        useCase.addRoleToMembers(members).thenAccept(success -> {
            EmbedBuilder winEmbed = new EmbedBuilder();
            winEmbed.setColor(new Color(36, 128, 70));
            winEmbed.setTitle("Тест пройден  :partying_face:");
            winEmbed.setDescription("Вы получили роль дебатер.");

            event.editMessageEmbeds(winEmbed.build()).setActionRow(Button.success(CLOSE_TEST_ID, "Закончить")).queue(
                    success1 -> {
                        testDataByUserMap.remove(event.getMember());
                        System.out.println("Сообщение о успешном прохождении теста изменено");
                    },
                    failure -> System.err.println("Не удалось изменить сообщение о успешном прохождении теста: " + failure.getMessage()));
        });
    }

    public void showTestFailed(ButtonInteractionEvent event) {
        TestDataByUser currentTestData = testDataByUserMap.get(event.getMember());
        EmbedBuilder lossEmbed = getTestFailedEmbed(currentTestData);

        event.editMessageEmbeds(lossEmbed.build()).setActionRow(Button.danger(CLOSE_TEST_ID, "Закрыть")).queue(
                success -> {
                    lastTestAttemptByUser.put(Objects.requireNonNull(event.getMember()).getIdLong(), System.currentTimeMillis());
                    testDataByUserMap.remove(event.getMember());
                    System.out.println("Сообщение о неудачном прохождении теста изменено");
                },
                failure -> System.err.println("Не удалось изменить сообщение о неудачном прохождении теста: " + failure.getMessage()));
    }

    public void showTestFailed(Member member, InteractionHook hook) {
        TestDataByUser currentTestData = testDataByUserMap.get(member);
        EmbedBuilder lossEmbed = getTestFailedEmbed(currentTestData);

        hook.editOriginalEmbeds(lossEmbed.build()).setActionRow(Button.danger(CLOSE_TEST_ID, "Закрыть")).queue(
                success -> {
                    lastTestAttemptByUser.put(Objects.requireNonNull(member).getIdLong(), System.currentTimeMillis());
                    testDataByUserMap.remove(member);
                    System.out.println("Сообщение о неудачном прохождении теста изменено");
                },
                failure -> System.err.println("Не удалось изменить сообщение о неудачном прохождении теста: " + failure.getMessage()));
    }

    public void showTestFailed(Member member, Message message) {
        TestDataByUser currentTestData = testDataByUserMap.get(member);
        EmbedBuilder lossEmbed = getTestFailedEmbed(currentTestData);

        message.editMessageEmbeds(lossEmbed.build()).setActionRow(Button.danger(CLOSE_TEST_ID, "Закрыть")).queue(
                success -> {
                    lastTestAttemptByUser.put(Objects.requireNonNull(member).getIdLong(), System.currentTimeMillis());
                    testDataByUserMap.remove(member);
                    System.out.println("Сообщение о неудачном прохождении теста изменено");
                },
                failure -> System.err.println("Не удалось изменить сообщение о неудачном прохождении теста: " + failure.getMessage()));

    }

    public EmbedBuilder getTestFailedEmbed(TestDataByUser currentTestData) {
        EmbedBuilder lossEmbed = new EmbedBuilder();
        lossEmbed.setColor(new Color(158, 26, 26));
        lossEmbed.setTitle("Тест провален :cry:");
        lossEmbed.setDescription("- Вы ответили правильно на " + (currentTestData.getCurrentQuestionNumber() - 1) + " из " + MAX_QUESTIONS + " вопросов.\n" +
                "- Перепройди тест через 10 минут.");
        return lossEmbed;
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
        System.out.println("Дебаты завершены");
    }

    public void endDebateWithOutWinner() {
        if (debateStartTask != null && !debateStartTask.isDone()) {
            debateStartTask.cancel(true);
        }

        isDebateStarted = false;

        subscribeDebatersList.clear();
        subscribeJudgesList.clear();

        showSubscribeMessage();
        System.out.println("Дебаты завершены");
    }

    private boolean isMemberNotInWaitingRoom(AudioChannelUnion voiceChannel) {
        return voiceChannel.getIdLong() != VoiceChannelsID.WAITING_ROOM;
    }

    private boolean isMemberHasDebaterRole(Member member) {
        return member.getRoles().stream().anyMatch(role -> debatersRuleIds.contains(role.getIdLong()));
    }

//    private boolean isMemberHasJudgeRole(Member member) {
//        return member.getRoles().stream().anyMatch(role -> role.getIdLong() == RolesID.JUDGE_APF);
//    }

    private boolean isMemberInList(String list, Member member) {
        return list != null && list.contains(member.getAsMention());
    }

    private static <K, V> void removeFirstEntry(LinkedHashMap<K, V> map) {
        Iterator<K> iterator = map.keySet().iterator();
        if (iterator.hasNext()) {
            K firstKey = iterator.next();
            map.remove(firstKey);
        }
    }

}
