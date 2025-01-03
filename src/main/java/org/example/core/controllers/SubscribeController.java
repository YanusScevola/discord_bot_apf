package org.example.core.controllers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

import org.example.core.constants.*;
import org.example.core.models.AwaitingTestUser;
import org.example.core.models.Question;
import org.example.core.models.TestDataByUser;
import org.example.core.models.Theme;
import org.example.resources.Colors;
import org.example.resources.StringRes;
import org.example.domain.UseCase;
import org.jetbrains.annotations.NotNull;

public class SubscribeController {
//    private static final int DEBATERS_LIMIT = 1; //4
//    private static final int JUDGES_LIMIT = 1; //1
//    private static final int START_DEBATE_TIMER = 5;
//    private static final int TEST_TIMER = 20;

    private static final int DEBATERS_LIMIT = 4;
    private static final int JUDGES_LIMIT = 1;
    private static final int START_DEBATE_TIMER = 30;
    private static final int TEST_TIMER = 20;

    private static final int MAX_QUESTIONS = 6;

    private static final String DEBATER_SUBSCRIBE_BTN_ID = "debater_subscribe";
    private static final String JUDGE_SUBSCRIBE_BTN_ID = "judge_subscribe";
    private static final String UNSUBSCRIBE_BTN_ID = "unsubscribe";
    private static final String START_TEST_ID = "start_test";
    private static final String ANSWER_A_ID = "answer_a";
    private static final String ANSWER_B_ID = "answer_b";
    private static final String ANSWER_C_ID = "answer_c";
    private static final String ANSWER_D_ID = "answer_d";
    private static final String CLOSE_TEST_ID = "close_test";

    private final TextChannel channel;
    private final UseCase useCase;
    private final RatingController ratingController;
    private final HistoryController historyController;
    private Message messageDebateStart;

    public DebateController debateController;
    private ScheduledFuture<?> debateStartTask;

    private long timerForStartDebate = 0;
    private boolean isDebateStarted = false;

    private final List<Long> debaterRoles = new ArrayList<>(Arrays.asList(
            RolesID.HEAD_GOVERNMENT,
            RolesID.HEAD_OPPOSITION,
            RolesID.MEMBER_GOVERNMENT,
            RolesID.MEMBER_OPPOSITION
    ));

    private final List<String> voiceChannelsNames = new ArrayList<>(Arrays.asList(
            StringRes.CHANNEL_JUDGE,
            StringRes.CHANNEL_TRIBUNE,
            StringRes.CHANNEL_GOVERNMENT,
            StringRes.CHANNEL_OPPOSITION
    ));

    private List<Long> debatersRuleIds = new ArrayList<>(Arrays.asList(
            RolesID.DEBATER_APF_1,
            RolesID.DEBATER_APF_2,
            RolesID.DEBATER_APF_3,
            RolesID.DEBATER_APF_4,
            RolesID.DEBATER_APF_5
    ));


    private final List<Member> subscribeDebatersList = new ArrayList<>();
    private final List<Member> subscribeJudgesList = new ArrayList<>();
    private final LinkedHashMap<Member, TestDataByUser> testDataByUserMap = new LinkedHashMap<>();
    private final Map<Long, InteractionHook> memberByNeedToStartTestHook = new ConcurrentHashMap<>();

    private static final Map<String, Integer> answerButtonIdByAnswersIndex = new HashMap<>();

    static {
        answerButtonIdByAnswersIndex.put(ANSWER_A_ID, 0);
        answerButtonIdByAnswersIndex.put(ANSWER_B_ID, 1);
        answerButtonIdByAnswersIndex.put(ANSWER_C_ID, 2);
        answerButtonIdByAnswersIndex.put(ANSWER_D_ID, 3);
    }


    public SubscribeController(UseCase useCase) {
        this.useCase = useCase;
        this.channel = useCase.getTextChannel(TextChannelsID.SUBSCRIBE);
        this.ratingController = RatingController.getInstance(useCase);
        this.historyController = HistoryController.getInstance(useCase);

        showSubscribeMessage();
        ratingController.displayDebatersList();
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        switch (event.getComponentId()) {
            case DEBATER_SUBSCRIBE_BTN_ID:
                onClickDebaterSubscribeBtn(event, member);
                break;
            case JUDGE_SUBSCRIBE_BTN_ID:
                onClickJudgeSubscribeBtn(event, member);
                break;
            case UNSUBSCRIBE_BTN_ID:
                onClickUnsubscribe(event, member);
                break;
            case START_TEST_ID:
                onClickStartTest(event, member);
                break;
            case ANSWER_A_ID:
                onClickAnswer(event, member);
                break;
            case ANSWER_B_ID:
                onClickAnswer(event, member);
                break;
            case ANSWER_C_ID:
                onClickAnswer(event, member);
                break;
            case ANSWER_D_ID:
                onClickAnswer(event, member);
                break;
            case CLOSE_TEST_ID:
                onClickCloseTest(event, member);
                break;
            default:
                // Можно добавить обработку неизвестных или неожиданных ID компонентов
                break;
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
        embedBuilder.setColor(Colors.BLUE);
//        embedBuilder.setTitle(StringRes.TITLE_DEBATE_SUBSCRIBE);
        embedBuilder.setDescription("Чтобы дебаты начались, необходимо \nнабрать " + DEBATERS_LIMIT + " дебатеров и минимум "
                + JUDGES_LIMIT + " судья.");
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

            if (subscribeDebatersList.size() >= DEBATERS_LIMIT) {
                message.editOriginal("Лимит дебатеров уже достигнут. Вы не можете подписаться.").queue();
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

            if (!isMemberHasJudgeRole(event.getMember())) {
                message.editOriginal("Нужно получить роль <@&" + RolesID.DEBATER_APF_3 + "> 3-го уровня и выше.").queue();
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
        useCase.removeOverdueAwaitingTestUser().join();
        useCase.getAwaitingTestUser(member.getIdLong(), AwaitingTestID.APF_TEST).thenAccept(awaitingTestUser -> {
            long currentTimeMillis = System.currentTimeMillis();
            boolean hasAttempt = awaitingTestUser != null;
            Timestamp coolDownEndTime = hasAttempt ? awaitingTestUser.getTime() : null;
            boolean isCoolDownPassed = coolDownEndTime == null || coolDownEndTime.getTime() <= currentTimeMillis;

            if (hasAttempt && !isCoolDownPassed) {
                long coolDownEndUnix = coolDownEndTime.getTime() / 1000;
                String textMessage = String.format("Вы можете начать тест заново только <t:%d:R> после последней попытки.", coolDownEndUnix);
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

            event.deferReply(true).queue(loadingHook -> {
                useCase.getAllQuestions().thenAccept(questions -> {
                    loadingHook.deleteOriginal().queue();
                    showFirstQuestion(member, questions);
                });
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
                success -> System.out.println("Сообщение о закрытии теста удалено"),
                failure -> System.err.println("Не удалось удалить сообщение о закрытии теста: " + failure.getMessage())
        );
    }

    private void startDebateTimer(long messageId, EmbedBuilder embed) {
        timerForStartDebate = System.currentTimeMillis() / 1000L + START_DEBATE_TIMER;
        embed.setDescription("**Внимание дебатеры!**\n- Через " + START_DEBATE_TIMER + " секунд вас перекинут по голосовым каналам чтобы вы могли подготовить аргументы и стратегию победы.\n- Тема дебатов будет доступна в етом чате или в чате Трибуны после начала подготоки.");
        embed.addField("Подгатовка дебатеров начнется через: ", "<t:" + timerForStartDebate + ":R>", false);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        debateStartTask = scheduler.schedule(() -> {
            if (System.currentTimeMillis() / 1000L >= timerForStartDebate) {
                channel.retrieveMessageById(messageId).queue(message -> {
                    messageDebateStart = message;
//                    updateStartDebateMessage(message);

                    timerForStartDebate = 0;
                    isDebateStarted = true;
                    debateController = new DebateController(useCase, this);

                    setupDebateRoles(subscribeDebatersList, subscribeJudgesList, () -> {
                        useCase.getVoiceChannel(VoiceChannelsID.WAITING_ROOM).thenAccept(waitingRoomChannel -> {
                            moveBotToVoiceChannel(waitingRoomChannel);
                            createVoiceChannels(voiceChannelsNames, () -> {
                                debateController.startDebate(message.getGuild());
                            });
                        });
                    });
                });
            }
        }, START_DEBATE_TIMER, TimeUnit.SECONDS);
    }

    public void updateDebaterPreparationMessage(Theme currentTheme, int debatersPreparationTime) {
        EmbedBuilder embedBuilder = new EmbedBuilder(messageDebateStart.getEmbeds().get(0));
        embedBuilder.clearFields();

        String debaterListString = subscribeDebatersList.stream()
                .map(Member::getAsMention)
                .collect(Collectors.joining("\n"));
        String judgeListString = subscribeJudgesList.stream()
                .map(Member::getAsMention)
                .collect(Collectors.joining("\n"));

        debaterListString = debaterListString.isEmpty() ? StringRes.DESCRIPTION_NO_MEMBERS : debaterListString;
        judgeListString = judgeListString.isEmpty() ? StringRes.DESCRIPTION_NO_MEMBERS : judgeListString;

        long currentTime = Instant.now().getEpochSecond();
        long eventStartTime = currentTime + debatersPreparationTime;

        embedBuilder.setDescription("**Тема:** " + currentTheme.getName() + "\n\n")
                .addField(StringRes.TITLE_DEBATER_LIST, debaterListString, true)
                .addField(StringRes.TITLE_JUDGES_LIST, judgeListString, true).addField(" ㅤ  ㅤ ", " ㅤ  ㅤ ", true)
                .addField("Подготовка дебатеров: ", "<t:" + eventStartTime + ":R>", false);

        Message updatedMessage = channel.editMessageEmbedsById(messageDebateStart.getId(), embedBuilder.build())
                .setActionRow(
                        Button.primary(DEBATER_SUBSCRIBE_BTN_ID, StringRes.BUTTON_SUBSCRIBE_DEBATER).asDisabled(),
                        Button.primary(JUDGE_SUBSCRIBE_BTN_ID, StringRes.BUTTON_SUBSCRIBE_JUDGE).asDisabled(),
                        Button.danger(UNSUBSCRIBE_BTN_ID, StringRes.BUTTON_UNSUBSCRIBE).asDisabled()
                ).complete();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        String finalDebaterListString = debaterListString;
        String finalJudgeListString = judgeListString;
        scheduler.schedule(() -> {
            EmbedBuilder finalEmbedBuilder = new EmbedBuilder(updatedMessage.getEmbeds().get(0))
                    .clearFields()
                    .setDescription("**Тема:** " + currentTheme.getName() + "\n\n")
                    .addField(StringRes.TITLE_DEBATER_LIST, finalDebaterListString, true)
                    .addField(StringRes.TITLE_JUDGES_LIST, finalJudgeListString, true)
                    .addField("Дебаты начались!", "", false);

            updatedMessage.editMessageEmbeds(finalEmbedBuilder.build()).queue();
            scheduler.shutdown();
        }, debatersPreparationTime, TimeUnit.SECONDS);
    }


    private void createVoiceChannels(List<String> channelNames, Runnable callback) {
        Category category = useCase.getCategoryByID(CategoriesID.DEBATE_CATEGORY).join();
        if (category != null) {
            Guild guild = category.getGuild();
            Role everyoneRole = guild.getPublicRole();
            List<VoiceChannel> existingChannels = category.getVoiceChannels();

            AtomicInteger completedChannelsCount = new AtomicInteger(0); // Счетчик созданных каналов
            int totalChannels = channelNames.size(); // Общее количество каналов для создания


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
                    allowPermissions.put(RolesID.JUDGE, Collections.singletonList(Permission.VOICE_CONNECT));
                    denyPermissions.put(everyoneRole.getIdLong(), Collections.singletonList(Permission.VOICE_CONNECT));
                } else if (isTribune) {
                    allowPermissions.put(everyoneRole.getIdLong(), Collections.singletonList(Permission.VOICE_CONNECT));
                    allowPermissions.put(1193537115003301971L, Collections.singletonList(Permission.VOICE_SPEAK));
                    allowPermissions.put(RolesID.HEAD_GOVERNMENT, Collections.singletonList(Permission.VOICE_SPEAK));
                    allowPermissions.put(RolesID.HEAD_OPPOSITION, Collections.singletonList(Permission.VOICE_SPEAK));
                    allowPermissions.put(RolesID.MEMBER_GOVERNMENT, Collections.singletonList(Permission.VOICE_SPEAK));
                    allowPermissions.put(RolesID.MEMBER_OPPOSITION, Collections.singletonList(Permission.VOICE_SPEAK));
                    allowPermissions.put(RolesID.JUDGE, Collections.singletonList(Permission.VOICE_SPEAK));
                    denyPermissions.put(everyoneRole.getIdLong(), Arrays.asList(Permission.VOICE_SPEAK, Permission.MESSAGE_SEND));
                } else if (isGovernment) {
                    allowPermissions.put(RolesID.HEAD_GOVERNMENT, Collections.singletonList(Permission.VOICE_CONNECT));
                    allowPermissions.put(RolesID.MEMBER_GOVERNMENT, Collections.singletonList(Permission.VOICE_CONNECT));
                    denyPermissions.put(everyoneRole.getIdLong(), Collections.singletonList(Permission.VOICE_CONNECT));
                } else if (isOpposition) {
                    allowPermissions.put(RolesID.HEAD_OPPOSITION, Collections.singletonList(Permission.VOICE_CONNECT));
                    allowPermissions.put(RolesID.MEMBER_OPPOSITION, Collections.singletonList(Permission.VOICE_CONNECT));
                    denyPermissions.put(everyoneRole.getIdLong(), Collections.singletonList(Permission.VOICE_CONNECT));
                }

                category.createVoiceChannel(channelName)
                        .addRolePermissionOverride(everyoneRole.getIdLong(), allowPermissions.get(everyoneRole.getIdLong()), denyPermissions.get(everyoneRole.getIdLong()))
                        .addRolePermissionOverride(RolesID.JUDGE, allowPermissions.get(RolesID.JUDGE), denyPermissions.get(RolesID.JUDGE))
                        .addRolePermissionOverride(RolesID.JUDGE, allowPermissions.get(RolesID.JUDGE), denyPermissions.get(RolesID.JUDGE))
                        .addRolePermissionOverride(RolesID.HEAD_GOVERNMENT, allowPermissions.get(RolesID.HEAD_GOVERNMENT), denyPermissions.get(RolesID.HEAD_GOVERNMENT))
                        .addRolePermissionOverride(RolesID.HEAD_OPPOSITION, allowPermissions.get(RolesID.HEAD_OPPOSITION), denyPermissions.get(RolesID.HEAD_OPPOSITION))
                        .addRolePermissionOverride(RolesID.MEMBER_GOVERNMENT, allowPermissions.get(RolesID.MEMBER_GOVERNMENT), denyPermissions.get(RolesID.MEMBER_GOVERNMENT))
                        .addRolePermissionOverride(RolesID.MEMBER_OPPOSITION, allowPermissions.get(RolesID.MEMBER_OPPOSITION), denyPermissions.get(RolesID.MEMBER_OPPOSITION))
                        .queue(channel -> {
                            debateController.addChannel(channel);

                            if (completedChannelsCount.incrementAndGet() == totalChannels) {
                                callback.run();
                            }
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

        int debatersCount = Math.min(DEBATERS_LIMIT, subscribeDebatersList.size());
        for (int i = 0; i < debatersCount; i++) {
            Member member = subscribeDebatersList.get(i);
            boolean hasDebaterRole = member.getRoles().stream().anyMatch(role -> debaterRoles.contains(role.getIdLong()));
            if (!hasDebaterRole) membersToRolesMap.put(member, debaterRoles.get(i));
        }


        judgesList.forEach((member) -> {
            judgesToRolesMap.put(member, RolesID.JUDGE);
        });

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
                embedBuilder.setColor(Colors.BLUE);
//                embedBuilder.setTitle(StringRes.TITLE_DEBATE_SUBSCRIBE);
                embedBuilder.setDescription("" + "Чтобы дебаты начались, необходимо \nнабрать " + DEBATERS_LIMIT + " дебатера и минимум "
                        + JUDGES_LIMIT + " судья.");

                List<String> debaters = subscribeDebatersList.stream().map(Member::getAsMention).collect(Collectors.toList());
                String debaterListString = debaters.isEmpty() ? StringRes.DESCRIPTION_NO_MEMBERS : String.join("\n", debaters);
                embedBuilder.addField(StringRes.TITLE_DEBATER_LIST, debaterListString, true);

                List<String> judges = subscribeJudgesList.stream().map(Member::getAsMention).collect(Collectors.toList());
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
        embed.setColor(Colors.BLUE);
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
        embed.setColor(Colors.BLUE);
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
        embed.setColor(Colors.BLUE);
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
        Map<Member, Long> members = new HashMap<>();
        members.put(Objects.requireNonNull(event.getMember()), RolesID.DEBATER_APF_1);

        useCase.addRoleToMembers(members).thenAccept(success -> {
            EmbedBuilder winEmbed = new EmbedBuilder();
            winEmbed.setColor(Colors.GREEN);
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
                    useCase.addAwaitingTest(new AwaitingTestUser(event.getMember(), AwaitingTestID.APF_TEST, new Timestamp(System.currentTimeMillis())));
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
                    useCase.addAwaitingTest(new AwaitingTestUser(member, AwaitingTestID.APF_TEST, new Timestamp(System.currentTimeMillis())));
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
                    useCase.addAwaitingTest(new AwaitingTestUser(member, AwaitingTestID.APF_TEST, new Timestamp(System.currentTimeMillis())));
                    testDataByUserMap.remove(member);
                    System.out.println("Сообщение о неудачном прохождении теста изменено");
                },
                failure -> System.err.println("Не удалось изменить сообщение о неудачном прохождении теста: " + failure.getMessage()));

    }

    public EmbedBuilder getTestFailedEmbed(TestDataByUser currentTestData) {
        EmbedBuilder lossEmbed = new EmbedBuilder();
        lossEmbed.setColor(Colors.RED);
        lossEmbed.setTitle("Тест провален :cry:");
        lossEmbed.setDescription("- Вы ответили правильно на " + (currentTestData.getCurrentQuestionNumber() - 1) + " из " + MAX_QUESTIONS + " вопросов.\n" +
                "- Перепройди тест через 30 минут.");
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
        if (timerForStartDebate != 0 && (subscribeDebatersList.size() < DEBATERS_LIMIT || subscribeJudgesList.size() < JUDGES_LIMIT)) {
            cancelDebateStart(callback);
        } else {
            updateList(callback);
        }
    }


    private void removeJudgeFromList(Member member, Runnable callback) {
        subscribeJudgesList.remove(member);
        if (timerForStartDebate != 0 && (subscribeJudgesList.size() < JUDGES_LIMIT || subscribeDebatersList.size() < DEBATERS_LIMIT)) {
            cancelDebateStart(callback);
        } else {
            updateList(callback);
        }
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

    private boolean isMemberHasJudgeRole(Member member) {
        List<Long> judgeRoles = Arrays.asList(RolesID.DEBATER_APF_3, RolesID.DEBATER_APF_4, RolesID.DEBATER_APF_5);
        return member.getRoles().stream().anyMatch(role -> judgeRoles.contains(role.getIdLong()));
    }

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
