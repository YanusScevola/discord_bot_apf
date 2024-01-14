package org.example.ui.controllers;

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
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import org.example.resources.StringRes;
import org.example.ui.constants.CategoriesID;
import org.example.ui.constants.RolesID;
import org.example.ui.constants.TextChannelsID;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.ui.constants.VoiceChannelsID;
import org.jetbrains.annotations.NotNull;

public class SubscribeController {
    private static final String DEBATER_SUBSCRIBE_BTN_ID = "debater_subscribe";
    private static final String JUDGE_SUBSCRIBE_BTN_ID = "judge_subscribe";
    private static final String UNSUBSCRIBE_BTN_ID = "unsubscribe";

    private static final int DEBATERS_LIMIT = 1; //4
    private static final int JUDGES_LIMIT = 1; //1

    private static final int START_DEBATE_TIMER = 6; //60

    private final TextChannel channel;
    private final ApiRepository apiRepository;
    private final DbRepository dbRepository;
    private final StringRes stringsRes;

    private List<Long> debaterRoles = new ArrayList<>();
    private List<String> voiceChannelsNames = new ArrayList<>();
    private final List<Member> subscribeDebatersList = new ArrayList<>();
    private final List<Member> subscribeJudgesList = new ArrayList<>();

    private long timerForStartDebate = 0;
    private boolean isDebateStarted = false;
    public DebateController debateController;
    private ScheduledFuture<?> debateStartTask;

    public SubscribeController(ApiRepository apiRepository, DbRepository dbRepository, StringRes stringsRes) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        this.stringsRes = stringsRes;
        this.channel = apiRepository.getTextChannel(TextChannelsID.SUBSCRIBE);

        debaterRoles = List.of(
                RolesID.HEAD_GOVERNMENT,
                RolesID.HEAD_OPPOSITION,
                RolesID.MEMBER_GOVERNMENT,
                RolesID.MEMBER_OPPOSITION
        );

        voiceChannelsNames = List.of(
                stringsRes.get(StringRes.Key.CHANNEL_JUDGE),
                stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE),
                stringsRes.get(StringRes.Key.CHANNEL_GOVERNMENT),
                stringsRes.get(StringRes.Key.CHANNEL_OPPOSITION)
        );

        this.channel.getHistoryFromBeginning(1).queue(history -> {
            if (history.isEmpty()) {
                showSubscribeMessage();
            } else {
                List<Message> messages = history.getRetrievedHistory();
                this.channel.purgeMessages(messages);
                showSubscribeMessage();
            }
        });
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        Member member = Objects.requireNonNull(event.getMember());
        switch (event.getComponentId()) {
            case DEBATER_SUBSCRIBE_BTN_ID -> onClickDebaterSubscribeBtn(event, member);
            case JUDGE_SUBSCRIBE_BTN_ID -> onClickJudgeSubscribeBtn(event, member);
            case UNSUBSCRIBE_BTN_ID -> onClickUnsubscribe(event, member);
        }
    }

    public void onLeaveFromVoiceChannel(@NotNull GuildVoiceUpdateEvent event) {
        boolean isDebaterSubscriber = subscribeDebatersList.stream().anyMatch(user -> user.getId().equals(event.getMember().getId()));
        boolean isJudgeSubscriber = subscribeJudgesList.stream().anyMatch(user -> user.getId().equals(event.getMember().getId()));
        if (timerForStartDebate == 0) {
            if (!isDebateStarted) {
                if (isDebaterSubscriber) removeDebaterFromList(event.getMember(), () -> {
                });
                if (isJudgeSubscriber) removeJudgeFromList(event.getMember(), () -> {
                });
            }
        } else {
            if (isDebaterSubscriber) removeDebaterFromList(event.getMember(), () -> {
            });
            if (isJudgeSubscriber) removeJudgeFromList(event.getMember(), () -> {
            });
        }
    }

    private void showSubscribeMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(new Color(54, 57, 63));
        embedBuilder.setTitle(stringsRes.get(StringRes.Key.TITLE_DEBATE_SUBSCRIBE));
        embedBuilder.addField(stringsRes.get(StringRes.Key.TITLE_DEBATER_LIST), stringsRes.get(StringRes.Key.DESCRIPTION_NO_MEMBERS), true);
        embedBuilder.addField(stringsRes.get(StringRes.Key.TITLE_JUDGES_LIST), stringsRes.get(StringRes.Key.DESCRIPTION_NO_MEMBERS), true);

        Button debaterButton = Button.success(DEBATER_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_DEBATER));
        Button judgeButton = Button.primary(JUDGE_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_JUDGE));
        Button unsubscribeButton = Button.danger(UNSUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_UNSUBSCRIBE));

        channel.sendMessageEmbeds(embedBuilder.build()).setActionRow(debaterButton, judgeButton, unsubscribeButton).queue();
    }

    private void onClickDebaterSubscribeBtn(@NotNull ButtonInteractionEvent event, Member member) {
        apiRepository.showEphemeralLoading(event, (message) -> {
            if (event.getMember() != null) {
                AudioChannelUnion voiceChannel = Objects.requireNonNull(event.getMember().getVoiceState()).getChannel();
                if (voiceChannel != null) {
                    boolean isMemberInWaitingRoom = voiceChannel.getIdLong() == VoiceChannelsID.WAITING_ROOM;
                    boolean isMemberHasDebaterRole = event.getMember().getRoles().stream().anyMatch(role -> role.getIdLong() == RolesID.DEBATER_APF);

                    if (isMemberInWaitingRoom) {
                        if (isMemberHasDebaterRole) {
                            if (subscribeDebatersList.contains(member)) {
                                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_ALREADY_DEBATER)).queue();
                            } else {
                                addDebaterToList(member, () -> {
                                    message.editOriginal(stringsRes.get(StringRes.Key.REMARK_DEBATER_ADDED)).queue();
                                });
                            }
                        } else message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_DEBATER_ROLE)).queue();
                    } else message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_WAITING_ROOM)).queue();
                } else message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_WAITING_ROOM)).queue();
            }
        });
    }

    private void onClickJudgeSubscribeBtn(@NotNull ButtonInteractionEvent event, Member member) {
        apiRepository.showEphemeralLoading(event, (message) -> {
            if (event.getMember() != null) {
                AudioChannelUnion voiceChannel = Objects.requireNonNull(event.getMember().getVoiceState()).getChannel();
                if (voiceChannel != null) {
                    boolean isMemberInWaitingRoom = voiceChannel.getIdLong() == VoiceChannelsID.WAITING_ROOM;
                    boolean isMemberHasJudgeRole = event.getMember().getRoles().stream().anyMatch(role -> role.getIdLong() == RolesID.JUDGE_APF);

                    if (isMemberInWaitingRoom) {
                        if (isMemberHasJudgeRole) {
                            if (subscribeJudgesList.contains(member)) {
                                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_ALREADY_JUDGE)).queue();
                            } else {
                                addJudgeToList(member, () -> {
                                    message.editOriginal(stringsRes.get(StringRes.Key.REMARK_JUDGE_ADDED)).queue();
                                });
                            }
                        } else message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_JUDGE_ROLE)).queue();
                    } else message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_WAITING_ROOM)).queue();
                } else message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_WAITING_ROOM)).queue();
            }
        });

    }

    private void onClickUnsubscribe(ButtonInteractionEvent event, Member member) {
        apiRepository.showEphemeralLoading(event, (message) -> {
            channel.getHistoryFromBeginning(1).queue(history -> {
                MessageEmbed embed = history.getRetrievedHistory().get(0).getEmbeds().get(0);
                String debatersList = embed.getFields().get(0).getValue();
                String judgeList = embed.getFields().get(1).getValue();

                if (!history.isEmpty()) {
                    if (debatersList != null && debatersList.contains(member.getAsMention())) {
                        removeDebaterFromList(member, () -> {
                            message.editOriginal(stringsRes.get(StringRes.Key.REMARK_DEBATER_REMOVED)).queue();
                        });
                    } else if (judgeList != null && judgeList.contains(member.getAsMention())) {
                        removeJudgeFromList(member, () -> {
                            message.editOriginal(stringsRes.get(StringRes.Key.REMARK_JUDGE_REMOVED)).queue();
                        });
                    } else message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_SUBSCRIBED)).queue();
                } else message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_SUBSCRIBED)).queue();

            });
        });
    }

    private void update(Runnable callback) {
        channel.getHistoryFromBeginning(1).queue(history -> {
            if (!history.isEmpty()) {
                Message message = history.getRetrievedHistory().get(0);
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(new Color(54, 57, 63));
                embedBuilder.setTitle(stringsRes.get(StringRes.Key.TITLE_DEBATE_SUBSCRIBE));

                List<String> debaters = subscribeDebatersList.stream().map(Member::getAsMention).toList();
                String debaterListString = debaters.isEmpty() ? stringsRes.get(StringRes.Key.DESCRIPTION_NO_MEMBERS) : String.join("\n", debaters);
                embedBuilder.addField(stringsRes.get(StringRes.Key.TITLE_DEBATER_LIST), debaterListString, true);

                List<String> judges = subscribeJudgesList.stream().map(Member::getAsMention).toList();
                String judgeListString = judges.isEmpty() ? stringsRes.get(StringRes.Key.DESCRIPTION_NO_MEMBERS) : String.join("\n", judges);
                embedBuilder.addField(stringsRes.get(StringRes.Key.TITLE_JUDGES_LIST), judgeListString, true);

                Button debaterButton = Button.success(DEBATER_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_DEBATER));
                Button judgeButton = Button.primary(JUDGE_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_JUDGE));
                Button unsubscribeButton = Button.danger(UNSUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_UNSUBSCRIBE));

                if (subscribeDebatersList.size() >= DEBATERS_LIMIT && subscribeJudgesList.size() >= JUDGES_LIMIT && timerForStartDebate == 0) {
                    startDebateTimer(message.getIdLong(), embedBuilder);
                }

                channel.editMessageEmbedsById(message.getId(), embedBuilder.build()).setActionRow(debaterButton, judgeButton, unsubscribeButton).queue((msg) -> {
                    if (callback != null) callback.run();
                });
            }
        });
    }

    private void startDebateTimer(long messageId, EmbedBuilder embed) {
        timerForStartDebate = System.currentTimeMillis() / 1000L + START_DEBATE_TIMER;
        String timerMessage = "<t:" + timerForStartDebate + ":R>";
        embed.addField(stringsRes.get(StringRes.Key.TITLE_TIMER), timerMessage, false);

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        debateStartTask = scheduler.schedule(() -> channel.retrieveMessageById(messageId).queue(message -> {
            if (System.currentTimeMillis() / 1000L >= timerForStartDebate && timerForStartDebate != 0) {
                EmbedBuilder embedBuilder = new EmbedBuilder(message.getEmbeds().get(0));

                embedBuilder.getFields().stream().filter(field ->
                        Objects.equals(field.getName(), stringsRes.get(StringRes.Key.TITLE_TIMER))
                ).findFirst().ifPresent(field ->
                        embedBuilder.addField(Objects.requireNonNull(field.getName()), stringsRes.get(StringRes.Key.DESCRIPTION_NEED_GO_TO_TRIBUNE), false)
                );

                if (embedBuilder.getFields().size() > 2) {
                    embedBuilder.getFields().remove(2);
                } else {
//                    Utils.sendLogError(apiRepository, "scheduleTimerEndUpdate", "Недостаточно полей для удаления.");
                }

                Button debaterButton = Button.success(DEBATER_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_DEBATER)).asDisabled();
                Button judgeButton = Button.primary(JUDGE_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_JUDGE)).asDisabled();
                Button unsubscribeButton = Button.danger(UNSUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_UNSUBSCRIBE)).asDisabled();

                channel.editMessageEmbedsById(message.getId(), embedBuilder.build()).setActionRow(debaterButton, judgeButton, unsubscribeButton).queue();

                timerForStartDebate = 0;
                isDebateStarted = true;
                debateController = new DebateController(apiRepository, dbRepository, stringsRes, this);

                setupDebateRoles(subscribeDebatersList, subscribeJudgesList, () -> {
                    createVoiceChannels(voiceChannelsNames);
                });
            }

        }), START_DEBATE_TIMER, TimeUnit.SECONDS);
    }

    private void createVoiceChannels(List<String> channelNames) {
        Category category = apiRepository.getCategoryByID(CategoriesID.DEBATE_CATEGORY).join();
        if (category != null) {
            Guild guild = category.getGuild();
            Role everyoneRole = guild.getPublicRole();
            List<VoiceChannel> existingChannels = category.getVoiceChannels();

            for (String channelName : channelNames) {
                boolean isJudge = channelName.equals(stringsRes.get(StringRes.Key.CHANNEL_JUDGE));
                boolean isTribune = channelName.equals(stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE));
                boolean isGovernment = channelName.equals(stringsRes.get(StringRes.Key.CHANNEL_GOVERNMENT));
                boolean isOpposition = channelName.equals(stringsRes.get(StringRes.Key.CHANNEL_OPPOSITION));

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
//            Utils.sendLogError(apiRepository, "createVoiceChannels", "Категория не найдена. Проверьте ID.");
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

        apiRepository.addRolesToMembers(membersToRolesMap, () -> {
            apiRepository.addRolesToMembers(judgesToRolesMap, callback);
        });
    }


    private void moveBotToVoiceChannel(VoiceChannel channel) {
        if (channel != null) {
            Guild guild = channel.getGuild();
            AudioManager audioManager = guild.getAudioManager();
            if (!audioManager.isConnected()) {
                audioManager.openAudioConnection(channel);
//                Utils.sendLogDebug(apiRepository, "joinVoiceChannel", "Бот подключен к голосовому каналу: " +
//                channel.getName());
            } else {
//                Utils.sendLogError(apiRepository, "joinVoiceChannel", "Бот уже находится в голосовом канале или
//                пытается подключиться.");
            }
        } else {
//            Utils.sendLogError(apiRepository, "joinVoiceChannel", "Голосовой канал не найден.");
        }
    }

    private void addDebaterToList(Member member, Runnable callback) {
        subscribeDebatersList.add(member);
        subscribeJudgesList.remove(member);
        update(callback);
    }

    private void addJudgeToList(Member member, Runnable callback) {
        subscribeJudgesList.add(member);
        subscribeDebatersList.remove(member);
        update(callback);
    }

    private void removeDebaterFromList(Member member, Runnable callback) {
        subscribeDebatersList.remove(member);
        if (timerForStartDebate != 0 && subscribeDebatersList.size() < DEBATERS_LIMIT) {
            cancelDebateStart(callback);
        } else {
            update(callback);
        }

    }

    private void removeJudgeFromList(Member member, Runnable callback) {
        subscribeJudgesList.remove(member);
        update(callback);
    }

    private void cancelDebateStart(Runnable callback) {
        if (debateStartTask != null && !debateStartTask.isDone()) {
            debateStartTask.cancel(true);
            debateStartTask = null;

            timerForStartDebate = 0;
            isDebateStarted = false;
            update(callback);
        }
    }

    public void restartDebate() {
        cancelDebateStart(() -> {
            subscribeDebatersList.clear();
            subscribeJudgesList.clear();
            update(() -> {
            });
        });
    }
}
