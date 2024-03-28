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
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.managers.AudioManager;

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

    private final TextChannel channel;
    private final UseCase useCase;
    private final StringRes stringsRes;

    private final List<Long> debaterRoles;
    private final List<String> voiceChannelsNames;
    private final List<Member> subscribeDebatersList = new ArrayList<>();
    private final List<Member> subscribeJudgesList = new ArrayList<>();

    private long timerForStartDebate = 0;
    private boolean isDebateStarted = false;
    public DebateController debateController;
    private ScheduledFuture<?> debateStartTask;

    public SubscribeController(UseCase useCase, StringRes stringsRes) {
        this.useCase = useCase;
        this.stringsRes = stringsRes;
        this.channel = useCase.getTextChannel(TextChannelsID.SUBSCRIBE);

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

        showSubscribeMessage();
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
        embedBuilder.setColor(new Color(54, 57, 63));
        embedBuilder.setTitle(stringsRes.get(StringRes.Key.TITLE_DEBATE_SUBSCRIBE));
        embedBuilder.addField(stringsRes.get(StringRes.Key.TITLE_DEBATER_LIST), stringsRes.get(StringRes.Key.DESCRIPTION_NO_MEMBERS), true);
        embedBuilder.addField(stringsRes.get(StringRes.Key.TITLE_JUDGES_LIST), stringsRes.get(StringRes.Key.DESCRIPTION_NO_MEMBERS), true);

        Button debaterButton = Button.success(DEBATER_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_DEBATER));
        Button judgeButton = Button.primary(JUDGE_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_JUDGE));
        Button unsubscribeButton = Button.danger(UNSUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_UNSUBSCRIBE));

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
        useCase.showEphemeralLoading(event).thenAccept(message -> {
            if (event.getMember() == null) return;

            AudioChannelUnion voiceChannel = Objects.requireNonNull(event.getMember().getVoiceState()).getChannel();

            if (voiceChannel == null) {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_WAITING_ROOM)).queue();
                return;
            }

            if (isMemberNotInWaitingRoom(voiceChannel)) {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_WAITING_ROOM)).queue();
                return;
            }

            if (!isMemberHasDebaterRole(event.getMember())) {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_DEBATER_ROLE)).queue();
                return;
            }

            if (subscribeDebatersList.contains(member)) {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_ALREADY_DEBATER)).queue();
                return;
            }

            addDebaterToList(member, () -> message.editOriginal(stringsRes.get(StringRes.Key.REMARK_DEBATER_ADDED)).queue());
        });
    }

    private void onClickJudgeSubscribeBtn(@NotNull ButtonInteractionEvent event, Member member) {
        useCase.showEphemeralLoading(event).thenAccept(message -> {
            if (event.getMember() == null) return;

            AudioChannelUnion voiceChannel = Objects.requireNonNull(event.getMember().getVoiceState()).getChannel();

            if (voiceChannel == null || isMemberNotInWaitingRoom(voiceChannel)) {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_WAITING_ROOM)).queue();
                return;
            }

            if (!isMemberHasJudgeRole(event.getMember())) {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_JUDGE_ROLE)).queue();
                return;
            }

            if (subscribeJudgesList.contains(member)) {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_ALREADY_JUDGE)).queue();
                return;
            }

            addJudgeToList(member, () -> message.editOriginal(stringsRes.get(StringRes.Key.REMARK_JUDGE_ADDED)).queue());
        });
    }

    private void onClickUnsubscribe(ButtonInteractionEvent event, Member member) {
        useCase.showEphemeralLoading(event).thenAccept(message -> {
            channel.getHistoryFromBeginning(1).queue(history -> {
                if (history.isEmpty()) {
                    message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_SUBSCRIBED)).queue();
                    return;
                }

                MessageEmbed embed = history.getRetrievedHistory().get(0).getEmbeds().get(0);
                String debatersList = embed.getFields().get(0).getValue();
                String judgeList = embed.getFields().get(1).getValue();

                if (isMemberInList(debatersList, member)) {
                    removeDebaterFromList(member, () -> message.editOriginal(stringsRes.get(StringRes.Key.REMARK_DEBATER_REMOVED)).queue());
                } else if (isMemberInList(judgeList, member)) {
                    removeJudgeFromList(member, () -> message.editOriginal(stringsRes.get(StringRes.Key.REMARK_JUDGE_REMOVED)).queue());
                } else {
                    message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NEED_SUBSCRIBED)).queue();
                }
            });
        });
    }

    private void startDebateTimer(long messageId, EmbedBuilder embed) {
        timerForStartDebate = System.currentTimeMillis() / 1000L + START_DEBATE_TIMER;
        embed.addField(stringsRes.get(StringRes.Key.TITLE_TIMER), "<t:" + timerForStartDebate + ":R>", false);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        debateStartTask = scheduler.schedule(() -> {
            if (System.currentTimeMillis() / 1000L >= timerForStartDebate) {
                channel.retrieveMessageById(messageId).queue(message -> {
                    updateDebateMessage(message);
                    finalizeDebateStart();
                });
            }
        }, START_DEBATE_TIMER, TimeUnit.SECONDS);
    }

    private void updateDebateMessage(Message message) {
        EmbedBuilder embedBuilder = new EmbedBuilder(message.getEmbeds().get(0));
        embedBuilder.clearFields()
                .addField(stringsRes.get(StringRes.Key.TITLE_DEBATER_LIST), stringsRes.get(StringRes.Key.DESCRIPTION_NO_MEMBERS), true)
                .addField(stringsRes.get(StringRes.Key.TITLE_JUDGES_LIST), stringsRes.get(StringRes.Key.DESCRIPTION_NO_MEMBERS), true)
                .addField(stringsRes.get(StringRes.Key.TITLE_TIMER), stringsRes.get(StringRes.Key.DESCRIPTION_NEED_GO_TO_TRIBUNE), false);

        channel.editMessageEmbedsById(message.getId(), embedBuilder.build())
                .setActionRow(
                        Button.success(DEBATER_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_DEBATER)).asDisabled(),
                        Button.primary(JUDGE_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_JUDGE)).asDisabled(),
                        Button.danger(UNSUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_UNSUBSCRIBE)).asDisabled()
                ).queue();
    }

    private void finalizeDebateStart() {
        timerForStartDebate = 0;
        isDebateStarted = true;
        debateController = new DebateController(useCase, stringsRes, this);
        setupDebateRoles(subscribeDebatersList, subscribeJudgesList, () -> createVoiceChannels(voiceChannelsNames));
    }

    private void createVoiceChannels(List<String> channelNames) {
        Category category = useCase.getCategoryByID(CategoriesID.DEBATE_CATEGORY).join();
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
        subscribeDebatersList.clear();
        subscribeJudgesList.clear();
        showSubscribeMessage();
        isDebateStarted = false;
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
}
