package org.example.ui.channels;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
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
import org.example.utils.Utils;
import org.jetbrains.annotations.NotNull;

public class SubscribeTextChannel {
    TextChannel channel;
    ApiRepository apiRepository;
    DbRepository dbRepository;
    StringRes stringsRes;

    private static final String DEBATER_SUBSCRIBE_BTN_ID = "debater_subscribe";
    private static final String JUDGE_SUBSCRIBE_BTN_ID = "judge_subscribe";
    private static final String UNSUBSCRIBE_BTN_ID = "unsubscribe";

    private static final int DEBATERS_LIMIT = 1; //4
    private static final int JUDGES_LIMIT = 1; //1
    private static final int START_DEBATE_TIMER = 2; //60
    private static final int HELP_MESSAGE_TIME = 5; //5

    private long timerForStartDebate = 0;
    private boolean isDebateStarted = false;
    public DebateController debateController;


    List<User> debatersList = new ArrayList<>();
    List<User> judgesList = new ArrayList<>();


    public SubscribeTextChannel(ApiRepository apiRepository, DbRepository dbRepository, StringRes stringsRes) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        this.stringsRes = stringsRes;
        this.channel = apiRepository.getTextChannel(TextChannelsID.SUBSCRIBE);

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
        User user = Objects.requireNonNull(event.getMember()).getUser();
        switch (event.getComponentId()) {
            case DEBATER_SUBSCRIBE_BTN_ID -> onDebaterSubscribeClick(event, user);
            case JUDGE_SUBSCRIBE_BTN_ID -> onJudgeSubscribeClick(event, user);
            case UNSUBSCRIBE_BTN_ID -> onUnsubscribeClick(event, user);
        }
    }

    public void onLeaveFromVoiceChannel(@NotNull GuildVoiceUpdateEvent event) {
        boolean isDebaterSubscriber = debatersList.stream().anyMatch(user -> user.getId().equals(event.getMember().getId()));
        boolean isJudgeSubscriber = judgesList.stream().anyMatch(user -> user.getId().equals(event.getMember().getId()));
        if (timerForStartDebate == 0) {
            if (!isDebateStarted) {
                if (isDebaterSubscriber) removeDebaterFromList(event.getMember().getUser());
                if (isJudgeSubscriber && !isDebateStarted) removeJudgeFromList(event.getMember().getUser());
            }
        } else {
            if (isDebaterSubscriber) removeDebaterFromList(event.getMember().getUser());
            if (isJudgeSubscriber && !isDebateStarted) removeJudgeFromList(event.getMember().getUser());
        }

    }

    private void showSubscribeMessage() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(new Color(54, 57, 63));
        embedBuilder.setTitle(stringsRes.get(StringRes.Key.DEBATE_SUBSCRIBE_TITLE));
        embedBuilder.addField(stringsRes.get(StringRes.Key.DEBATER_LIST_TITLE), stringsRes.get(StringRes.Key.NO_MEMBERS), true);
        embedBuilder.addField(stringsRes.get(StringRes.Key.JUDGES_LIST_TITLE), stringsRes.get(StringRes.Key.NO_MEMBERS), true);

        Button debaterButton = Button.success(DEBATER_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_DEBATER));
        Button judgeButton = Button.primary(JUDGE_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_JUDGE));
        Button unsubscribeButton = Button.danger(UNSUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_UNSUBSCRIBE));

        channel.sendMessageEmbeds(embedBuilder.build()).setActionRow(debaterButton, judgeButton, unsubscribeButton).queue();
    }

    private void onDebaterSubscribeClick(@NotNull ButtonInteractionEvent event, User user) {
        if (event.getMember() != null) {
            if (event.getMember().getVoiceState() != null && event.getMember().getVoiceState().inAudioChannel() && Objects.equals(Objects.requireNonNull(event.getMember().getVoiceState().getChannel()).getId(), VoiceChannelsID.WAITING_ROOM)) {
                if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(RolesID.DEBATER_APF))) {
                    addDebaterToDb(user);
                    apiRepository.showEphemeralMessage(event, stringsRes.get(StringRes.Key.DEBATER_ADDED));
                } else {
                    apiRepository.showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_DEBATER_ROLE));
                }
            } else {
                apiRepository.showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_WAITING_ROOM));
            }
        }
    }

    private void onJudgeSubscribeClick(@NotNull ButtonInteractionEvent event, User user) {
        if (event.getMember() != null) {
            if (event.getMember().getVoiceState() != null && event.getMember().getVoiceState().inAudioChannel() && Objects.equals(Objects.requireNonNull(event.getMember().getVoiceState().getChannel()).getId(), VoiceChannelsID.WAITING_ROOM)) {
                if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(RolesID.JUDGE_APF))) {
                    addJudgeToDb(user);
                    apiRepository.showEphemeralMessage(event, stringsRes.get(StringRes.Key.JUDGE_ADDED));
                } else {
                    apiRepository.showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_JUDGE_ROLE));
                }
            } else {
                apiRepository.showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_WAITING_ROOM));
            }
        }
    }

    private void onUnsubscribeClick(ButtonInteractionEvent event, User user) {
        channel.getHistoryFromBeginning(1).queue(history -> {
            MessageEmbed embed = history.getRetrievedHistory().get(0).getEmbeds().get(0);
            String debatersList = embed.getFields().get(0).getValue();
            String judgeList = embed.getFields().get(1).getValue();

            if (!history.isEmpty()) {
                if (debatersList != null && debatersList.contains(user.getAsMention())) {
                    removeDebaterFromList(user);
                    apiRepository.showEphemeralMessage(event, stringsRes.get(StringRes.Key.DEBATER_REMOVED));
                } else if (judgeList != null && judgeList.contains(user.getAsMention())) {
                    removeJudgeFromList(user);
                    apiRepository.showEphemeralMessage(event, stringsRes.get(StringRes.Key.JUDGE_REMOVED));
                }
            }
            if (debatersList != null && debatersList.contains(user.getAsMention())) {
                removeDebaterFromList(user);
                apiRepository.showEphemeralMessage(event, stringsRes.get(StringRes.Key.DEBATER_REMOVED));
            } else {
                apiRepository.showEphemeralMessage(event, stringsRes.get(StringRes.Key.DEBATER_NOT_SUBSCRIBED));
            }
        });
    }

    private void update() {
        channel.getHistoryFromBeginning(1).queue(history -> {
            if (!history.isEmpty()) {
                Message message = history.getRetrievedHistory().get(0);
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setColor(new Color(54, 57, 63));
                embedBuilder.setTitle(stringsRes.get(StringRes.Key.DEBATE_SUBSCRIBE_TITLE));

                List<String> debaters = debatersList.stream().map(User::getAsMention).toList();
                String debaterListString = debaters.isEmpty() ? stringsRes.get(StringRes.Key.NO_MEMBERS) : String.join("\n", debaters);
                embedBuilder.addField(stringsRes.get(StringRes.Key.DEBATER_LIST_TITLE), debaterListString, true);

                List<String> judges = judgesList.stream().map(User::getAsMention).toList();
                String judgeListString = judges.isEmpty() ? stringsRes.get(StringRes.Key.NO_MEMBERS) : String.join("\n", judges);
                embedBuilder.addField(stringsRes.get(StringRes.Key.JUDGES_LIST_TITLE), judgeListString, true);

                if (debaters.size() >= DEBATERS_LIMIT && judges.size() >= JUDGES_LIMIT && timerForStartDebate == 0) {
                    timerForStartDebate = System.currentTimeMillis() / 1000L + START_DEBATE_TIMER;
                    String timerMessage = "<t:" + timerForStartDebate + ":R>";
                    embedBuilder.addField(stringsRes.get(StringRes.Key.TIMER_TITLE), timerMessage, false);
                    scheduleTimerEndUpdate(message.getIdLong());
                }

                Button debaterButton = Button.success(DEBATER_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_DEBATER));
                Button judgeButton = Button.primary(JUDGE_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_JUDGE));
                Button unsubscribeButton = Button.danger(UNSUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_UNSUBSCRIBE));

                channel.editMessageEmbedsById(message.getId(), embedBuilder.build()).setActionRow(debaterButton, judgeButton, unsubscribeButton).queue();
            }
        });
    }

    private void scheduleTimerEndUpdate(long messageId) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(() -> channel.retrieveMessageById(messageId).queue(message -> {
            if (System.currentTimeMillis() / 1000L >= timerForStartDebate && timerForStartDebate != 0) {
                EmbedBuilder embedBuilder = new EmbedBuilder(message.getEmbeds().get(0));

                embedBuilder.getFields().stream().filter(field -> Objects.equals(field.getName(), stringsRes.get(StringRes.Key.TIMER_TITLE))).findFirst().ifPresent(field -> embedBuilder.addField(Objects.requireNonNull(field.getName()), stringsRes.get(StringRes.Key.DEBATE_STARTED), false));
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
                debateController = new DebateController(apiRepository, dbRepository);
                createVoiceChannels();
                assignRoleToUser(debatersList, judgesList);

            }
        }), START_DEBATE_TIMER, TimeUnit.SECONDS);
    }

    private void createVoiceChannels() {
        Category category = apiRepository.getCategoryByID(CategoriesID.DEBATE_CATEGORY).join();
        if (category != null) {
            Guild guild = category.getGuild();
            Role everyoneRole = guild.getPublicRole();

            List<Permission> allow = new ArrayList<>();
            List<Permission> deny = new ArrayList<>(Collections.singletonList(Permission.VIEW_CHANNEL));
            List<Permission> allowForTribune = new ArrayList<>(Collections.singletonList(Permission.VIEW_CHANNEL));
            List<Permission> denyForTribune = new ArrayList<>();
            List<String> channelNames = List.of(stringsRes.get(StringRes.Key.JUDGE_CHANNEL_NAME), stringsRes.get(StringRes.Key.TRIBUNE_CHANNEL_NAME), stringsRes.get(StringRes.Key.GOVERNMENT_CHANNEL_NAME), stringsRes.get(StringRes.Key.OPPOSITION_CHANNEL_NAME));
            List<VoiceChannel> existingChannels = category.getVoiceChannels();

            for (String name : channelNames) {
                boolean isOpen = name.equals(stringsRes.get(StringRes.Key.TRIBUNE_CHANNEL_NAME));

                for (VoiceChannel existingChannel : existingChannels) {
                    if (existingChannel.getName().equalsIgnoreCase(name)) {
                        existingChannel.delete().queue();
                        break;
                    }
                }

                category.createVoiceChannel(name).addPermissionOverride(everyoneRole, isOpen ? allowForTribune : allow, isOpen ? denyForTribune : deny).queue(channel -> {
                    if (name.equals(stringsRes.get(StringRes.Key.TRIBUNE_CHANNEL_NAME))) {
                        moveBotToVoiceChannel(channel);
                    }
                    debateController.addChannel(channel);
                });
            }
        } else {
//            Utils.sendLogError(apiRepository, "createVoiceChannels", "Категория не найдена. Проверьте ID.");
        }
    }

    private void assignRoleToUser(List<User> debatersList, List<User> judgesList) {
        Collections.shuffle(debatersList);

        if (!debatersList.isEmpty()) {
            apiRepository.assignRoleToUser(debatersList.get(0).getId(), RolesID.HEAD_GOVERNMENT);
            if (debatersList.size() > 1) {
                apiRepository.assignRoleToUser(debatersList.get(1).getId(), RolesID.HEAD_OPPOSITION);
                for (int i = 2; i < debatersList.size(); i++) {
                    if (i % 2 == 0) {
                        apiRepository.assignRoleToUser(debatersList.get(i).getId(), RolesID.MEMBER_GOVERNMENT);
                    } else {
                        apiRepository.assignRoleToUser(debatersList.get(i).getId(), RolesID.MEMBER_OPPOSITION);
                    }
                }
            }
        }

        for (User user : judgesList) apiRepository.assignRoleToUser(user.getId(), RolesID.JUDGE);
    }

    private void moveBotToVoiceChannel(VoiceChannel channel) {
        if (channel != null) {
            Guild guild = channel.getGuild();
            AudioManager audioManager = guild.getAudioManager();
            if (!audioManager.isConnected() && !audioManager.isConnected()) {
                audioManager.openAudioConnection(channel);
//                Utils.sendLogDebug(apiRepository, "joinVoiceChannel", "Бот подключен к голосовому каналу: " + channel.getName());
            } else {
//                Utils.sendLogError(apiRepository, "joinVoiceChannel", "Бот уже находится в голосовом канале или пытается подключиться.");
            }
        } else {
//            Utils.sendLogError(apiRepository, "joinVoiceChannel", "Голосовой канал не найден.");
        }
    }

    private void addDebaterToDb(User user) {
        debatersList.add(user);
        judgesList.remove(user);
        update();
    }

    private void addJudgeToDb(User user) {
        judgesList.add(user);
        debatersList.remove(user);
        update();
    }

    private void removeDebaterFromList(User user) {
        debatersList.remove(user);
        update();
    }

    private void removeJudgeFromList(User user) {
        judgesList.remove(user);
        update();
    }

//

}
