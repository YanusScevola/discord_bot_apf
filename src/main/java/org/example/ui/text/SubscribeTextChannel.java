package org.example.ui.text;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.resources.StringsRes;
import org.example.ui.constants.RoleIsID;
import org.example.ui.constants.TextChannelsID;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.ui.constants.VoceChannelsID;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class SubscribeTextChannel {
    static TextChannel channel;
    ApiRepository apiRepository;
    DbRepository dbRepository;
    StringsRes stringsRes;

    private static final String DEBATER_SUBSCRIBE_ID = "debater_subscribe";
    private static final String JUDGE_SUBSCRIBE_ID = "judge_subscribe";
    private static final String UNSUBSCRIBE_ID = "unsubscribe";


    public SubscribeTextChannel(ApiRepository apiRepository, DbRepository dbRepository) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        this.stringsRes = StringsRes.getInstance(StringsRes.Language.ENGLISH);
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

        if (event.getComponentId().equals(SubscribeTextChannel.DEBATER_SUBSCRIBE_ID)) {
            if (event.getMember() != null) {
                if (event.getMember().getVoiceState() != null && event.getMember().getVoiceState().inAudioChannel() && Objects.equals(Objects.requireNonNull(event.getMember().getVoiceState().getChannel()).getId(), VoceChannelsID.WAITING_ROOM)) {
                    if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(RoleIsID.DEBATER))) {
                        addDebaterToList(event.getMember().getUser().getAsMention());
                        removeFromOtherList(userId, JUDGE_SUBSCRIBE_ID);
                        showEphemeralMessage(event, stringsRes.getString(StringsRes.Key.DEBATER_ADDED));
                    } else {
                        showEphemeralMessage(event, stringsRes.getString(StringsRes.Key.NEED_DEBATER_ROLE));
                    }
                } else {
                    showEphemeralMessage(event, stringsRes.getString(StringsRes.Key.NEED_WAITING_ROOM));
                }
            }
        } else if (event.getComponentId().equals(JUDGE_SUBSCRIBE_ID)) {
            if (event.getMember() != null) {
                if (event.getMember().getVoiceState() != null && event.getMember().getVoiceState().inAudioChannel() && Objects.equals(Objects.requireNonNull(event.getMember().getVoiceState().getChannel()).getId(), VoceChannelsID.WAITING_ROOM)) {
                    if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(RoleIsID.JUDGE))) {
                        addJudgeToList(userId);
                        removeFromOtherList(userId, DEBATER_SUBSCRIBE_ID);
                        showEphemeralMessage(event, "Вы были добавлены в список судей.");
                    } else {
                        showEphemeralMessage(event, "Нужно получить роль \"Судья\"");
                    }
                } else {
                    showEphemeralMessage(event, "Вы должны находиться в голосовом канале \"Зал ожидания\"");
                }
            }
        } else if (event.getComponentId().equals(UNSUBSCRIBE_ID)) {
            channel.getHistoryFromBeginning(1).queue(history -> {
                MessageEmbed embed = history.getRetrievedHistory().get(0).getEmbeds().get(0);
                String debatersList = embed.getFields().get(0).getValue();
                String judgeList = embed.getFields().get(1).getValue();

                if (!history.isEmpty())

                    if (debatersList != null && debatersList.contains(userId)) {
                        removeDebaterFromList(userId);
                        showEphemeralMessage(event, "Вы были удалены из списка дебатеров.");
                    } else if (judgeList != null && judgeList.contains(userId)) {
                        removeJudgeFromList(userId);
                        showEphemeralMessage(event, "Вы были удалены из списка судей.");
                    }
                if (debatersList != null && debatersList.contains(userId)) {
                    removeDebaterFromList(userId);
                    showEphemeralMessage(event, "Вы были удалены из списка дебатеров.");
                } else {
                    showEphemeralMessage(event, "Вы не зарегистрированы как дебатер.");
                }
            });
        }

    }

    private void removeFromOtherList(String userId, String otherListType) {
        channel.getHistoryFromBeginning(1).queue(history -> {
            if (!history.isEmpty()) {
                MessageEmbed embed = history.getRetrievedHistory().get(0).getEmbeds().get(0);
                String otherList;
                if (otherListType.equals(JUDGE_SUBSCRIBE_ID)) {
                    otherList = embed.getFields().get(1).getValue();
                } else {
                    otherList = embed.getFields().get(0).getValue();
                }

                if (otherList != null && otherList.contains(userId)) {
                    if (otherListType.equals(JUDGE_SUBSCRIBE_ID)) {
                        removeJudgeFromList(userId);
                    } else {
                        removeDebaterFromList(userId);
                    }
                }
            }
        });
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
        embedBuilder.setTitle("Запись на дебаты АПФ");
        embedBuilder.addField("Список дебатеров:", "Нет участников", true);
        embedBuilder.addField("Список судей:", "Нет участников", true);

        Button debaterButton = Button.success(DEBATER_SUBSCRIBE_ID, "Записаться как дебатер");
        Button judgeButton = Button.primary(JUDGE_SUBSCRIBE_ID, "Записаться как судья");
        Button unsubscribeButton = Button.danger(UNSUBSCRIBE_ID, "Отписаться");

        channel.sendMessageEmbeds(embedBuilder.build()).setActionRow(debaterButton, judgeButton, unsubscribeButton).queue();
    }

    public void addDebaterToList(String userMention) {
        channel.getHistoryFromBeginning(1).queue(history -> {
            if (!history.isEmpty()) {
                MessageEmbed embed = history.getRetrievedHistory().get(0).getEmbeds().get(0);
                EmbedBuilder embedBuilder = new EmbedBuilder(embed);

                String debatersList = embed.getFields().get(0).getValue();
                assert debatersList != null;
                if (debatersList.equals("Нет участников") || debatersList.trim().isEmpty()) {
                    debatersList = userMention;
                } else {
                    if (!debatersList.contains(userMention)) {
                        debatersList += "\n" + userMention;
                    }
                }

                embedBuilder.clearFields();
                embedBuilder.addField("Дебатеры:", debatersList, true);
                embedBuilder.addField("Судьи:", Objects.requireNonNull(embed.getFields().get(1).getValue()), true);

                channel.editMessageEmbedsById(history.getRetrievedHistory().get(0).getId(), embedBuilder.build()).queue();
            }
        });
    }

    public void addJudgeToList(String userMention) {
        channel.getHistoryFromBeginning(1).queue(history -> {
            if (!history.isEmpty()) {
                MessageEmbed embed = history.getRetrievedHistory().get(0).getEmbeds().get(0);
                EmbedBuilder embedBuilder = new EmbedBuilder(embed);

                String judgesList = embed.getFields().get(1).getValue();
                assert judgesList != null;
                if (judgesList.equals("Нет участников") || judgesList.trim().isEmpty()) {
                    judgesList = userMention;
                } else {
                    if (!judgesList.contains(userMention)) {
                        judgesList += "\n" + userMention;
                    }
                }

                embedBuilder.clearFields();
                embedBuilder.addField("Дебатеры:", Objects.requireNonNull(embed.getFields().get(0).getValue()), true);
                embedBuilder.addField("Судьи:", judgesList, true);

                channel.editMessageEmbedsById(history.getRetrievedHistory().get(0).getId(), embedBuilder.build()).queue();
            }
        });
    }


    public void removeJudgeFromList(String userMention) {
        channel.getHistoryFromBeginning(1).queue(history -> {
            if (!history.isEmpty()) {
                MessageEmbed embed = history.getRetrievedHistory().get(0).getEmbeds().get(0);
                EmbedBuilder embedBuilder = new EmbedBuilder(embed);

                String judgesList = embed.getFields().get(1).getValue(); // Получаем текущий список судей
                assert judgesList != null;
                if (judgesList.contains(userMention)) {
                    judgesList = judgesList.replace("\n" + userMention, "").replace(userMention, "");
                    if (judgesList.trim().isEmpty()) {
                        judgesList = "Нет участников";
                    }
                }

                embedBuilder.clearFields();
                embedBuilder.addField("Дебатеры:", Objects.requireNonNull(embed.getFields().get(0).getValue()), true);
                embedBuilder.addField("Судьи:", judgesList, true);

                channel.editMessageEmbedsById(history.getRetrievedHistory().get(0).getId(), embedBuilder.build()).queue();
            }
        });
    }


    public void removeDebaterFromList(String userMention) {
        channel.getHistoryFromBeginning(1).queue(history -> {
            if (!history.isEmpty()) {
                MessageEmbed embed = history.getRetrievedHistory().get(0).getEmbeds().get(0);
                EmbedBuilder embedBuilder = new EmbedBuilder(embed);

                String debatersList = embed.getFields().get(0).getValue();
                assert debatersList != null;
                if (debatersList.contains(userMention)) {
                    debatersList = debatersList.replace("\n" + userMention, "").replace(userMention, "");
                    if (debatersList.trim().isEmpty()) {
                        debatersList = "Нет участников";
                    }
                }
                embedBuilder.clearFields();
                embedBuilder.addField("Дебатеры:", debatersList, true);
                embedBuilder.addField("Судьи:", Objects.requireNonNull(embed.getFields().get(1).getValue()), true);
                channel.editMessageEmbedsById(history.getRetrievedHistory().get(0).getId(), embedBuilder.build()).queue();
            }
        });
    }

    private void showEphemeralMessage(ButtonInteractionEvent event, String message) {
        event.deferReply(true).queue(hook -> hook.sendMessage(message).queue(m -> m.delete().queueAfter(5, TimeUnit.SECONDS)));
    }


}
