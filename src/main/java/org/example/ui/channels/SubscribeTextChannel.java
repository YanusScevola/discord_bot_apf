package org.example.ui.channels;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.resources.StringRes;
import org.example.ui.constants.RoleIsID;
import org.example.ui.constants.TextChannelsID;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.ui.constants.VoceChannelsID;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SubscribeTextChannel {
    static TextChannel channel;
    ApiRepository apiRepository;
    DbRepository dbRepository;
    StringRes stringsRes;

    private static final String DEBATER_SUBSCRIBE_BTN_ID = "debater_subscribe";
    private static final String JUDGE_SUBSCRIBE_BTN_ID = "judge_subscribe";
    private static final String UNSUBSCRIBE_BTN_ID = "unsubscribe";

    private static final int DEBATERS_LIMIT = 1;
    private static final int JUDGES_LIMIT = 1;
    private static final int START_DEBATE_TIMER = 10;
    private static final int HELP_MESSAGE_DELETE_TIME = 5;

    List<String> debatersList = new ArrayList<>();
    List<String> judgesList = new ArrayList<>();


    public SubscribeTextChannel(ApiRepository apiRepository, DbRepository dbRepository) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        this.stringsRes = StringRes.getInstance(StringRes.Language.RUSSIAN);

        if (channel == null) channel = apiRepository.getTextChannel(TextChannelsID.SUBSCRIBE);

        channel.getHistoryFromBeginning(1).queue(history -> {
            if (history.isEmpty()) {
                showSubscribeMessage();
            } else {
                List<Message> messages = history.getRetrievedHistory();
                channel.purgeMessages(messages);
                showSubscribeMessage();
            }
        });
    }

    public void onButtonInteraction(ButtonInteractionEvent event) {
        String userId = Objects.requireNonNull(event.getMember()).getUser().getAsMention();

        if (event.getComponentId().equals(DEBATER_SUBSCRIBE_BTN_ID)) {
            onDebaterSubscribeClick(event, userId);
        } else if (event.getComponentId().equals(JUDGE_SUBSCRIBE_BTN_ID)) {
            onJudgeSubscribeClick(event, userId);
        } else if (event.getComponentId().equals(UNSUBSCRIBE_BTN_ID)) {
            onUnsubscribeClick(event, userId);
        }

    }

    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (Objects.requireNonNull(event.getChannelLeft()).getId().equals(VoceChannelsID.WAITING_ROOM)) {
            if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(RoleIsID.DEBATER))) {
                removeDebaterFromList(event.getMember().getUser().getAsMention());
            }
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

    private void onDebaterSubscribeClick(ButtonInteractionEvent event, String userId) {
        if (event.getMember() != null) {
            if (event.getMember().getVoiceState() != null && event.getMember().getVoiceState().inAudioChannel() && Objects.equals(Objects.requireNonNull(event.getMember().getVoiceState().getChannel()).getId(), VoceChannelsID.WAITING_ROOM)) {
                if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(RoleIsID.DEBATER))) {
                    addDebaterToDb(userId);
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.DEBATER_ADDED));
                } else {
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_DEBATER_ROLE));
                }
            } else {
                showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_WAITING_ROOM));
            }
        }
    }

    private void onJudgeSubscribeClick(ButtonInteractionEvent event, String userId) {
        if (event.getMember() != null) {
            if (event.getMember().getVoiceState() != null && event.getMember().getVoiceState().inAudioChannel() && Objects.equals(Objects.requireNonNull(event.getMember().getVoiceState().getChannel()).getId(), VoceChannelsID.WAITING_ROOM)) {
                if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(RoleIsID.JUDGE))) {
                    addJudgeToDb(userId);
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.JUDGE_ADDED));
                } else {
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_JUDGE_ROLE));
                }
            } else {
                showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_WAITING_ROOM));
            }
        }
    }

    private void onUnsubscribeClick(ButtonInteractionEvent event, String userId) {
        channel.getHistoryFromBeginning(1).queue(history -> {
            MessageEmbed embed = history.getRetrievedHistory().get(0).getEmbeds().get(0);
            String debatersList = embed.getFields().get(0).getValue();
            String judgeList = embed.getFields().get(1).getValue();

            if (!history.isEmpty()) {
                if (debatersList != null && debatersList.contains(userId)) {
                    removeDebaterFromList(userId);
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.DEBATER_REMOVED));
                } else if (judgeList != null && judgeList.contains(userId)) {
                    removeJudgeFromList(userId);
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.JUDGE_REMOVED));
                }
            }
            if (debatersList != null && debatersList.contains(userId)) {
                removeDebaterFromList(userId);
                showEphemeralMessage(event, stringsRes.get(StringRes.Key.DEBATER_REMOVED));
            } else {
                showEphemeralMessage(event, stringsRes.get(StringRes.Key.DEBATER_NOT_SUBSCRIBED));
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

                String debaterListString = debatersList.isEmpty() ? stringsRes.get(StringRes.Key.NO_MEMBERS) : String.join("\n", debatersList);
                embedBuilder.addField(stringsRes.get(StringRes.Key.DEBATER_LIST_TITLE), debaterListString, true);

                String judgeListString = judgesList.isEmpty() ? stringsRes.get(StringRes.Key.NO_MEMBERS) : String.join("\n", judgesList);
                embedBuilder.addField(stringsRes.get(StringRes.Key.JUDGES_LIST_TITLE), judgeListString, true);

                Button debaterButton = Button.success(DEBATER_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_DEBATER));
                Button judgeButton = Button.primary(JUDGE_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_JUDGE));
                Button unsubscribeButton = Button.danger(UNSUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_UNSUBSCRIBE));

                if (debatersList.size() >= DEBATERS_LIMIT && judgesList.size() >= JUDGES_LIMIT) {
                    // startTimer();
                }

                channel.editMessageEmbedsById(message.getId(), embedBuilder.build()).setActionRow(debaterButton, judgeButton, unsubscribeButton).queue();
            }
        });
    }

    private void addDebaterToDb(String userId) {
        debatersList.add(userId);
        judgesList.remove(userId);
        update();
    }

    private void addJudgeToDb(String userId) {
        judgesList.add(userId);
        debatersList.remove(userId);
        update();
    }

    private void removeDebaterFromList(String userId) {
        debatersList.remove(userId);
        update();
    }

    private void removeJudgeFromList(String userId) {
        judgesList.remove(userId);
        update();
    }

    private void showEphemeralMessage(ButtonInteractionEvent event, String message) {
        event.deferReply(true).queue(hook -> hook.sendMessage(message)
                .queue(m -> m.delete().queueAfter(HELP_MESSAGE_DELETE_TIME, TimeUnit.SECONDS)));
    }


}
