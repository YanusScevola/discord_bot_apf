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
import org.example.resources.StringRes;
import org.example.ui.constants.CategoriesID;
import org.example.ui.constants.RoleIsID;
import org.example.ui.constants.TextChannelsID;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.ui.constants.VoceChannelsID;
import org.jetbrains.annotations.NotNull;

public class SubscribeTextChannel {
    TextChannel channel;
    ApiRepository apiRepository;
    DbRepository dbRepository;
    StringRes stringsRes;

    private static final String DEBATER_SUBSCRIBE_BTN_ID = "debater_subscribe";
    private static final String JUDGE_SUBSCRIBE_BTN_ID = "judge_subscribe";
    private static final String UNSUBSCRIBE_BTN_ID = "unsubscribe";

    private static final int DEBATERS_LIMIT = 1;
    private static final int JUDGES_LIMIT = 1;
    private static final int START_DEBATE_TIMER = 5;
    private static final int HELP_MESSAGE_TIME = 5;

    private long timerEnd = 0;

    List<User> debatersList = new ArrayList<>();
    List<User> judgesList = new ArrayList<>();


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

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        User user = Objects.requireNonNull(event.getMember()).getUser();
        if (event.getComponentId().equals(DEBATER_SUBSCRIBE_BTN_ID)) {
            onDebaterSubscribeClick(event, user);
        } else if (event.getComponentId().equals(JUDGE_SUBSCRIBE_BTN_ID)) {
            onJudgeSubscribeClick(event, user);
        } else if (event.getComponentId().equals(UNSUBSCRIBE_BTN_ID)) {
            onUnsubscribeClick(event, user);
        }
    }

    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        if (Objects.requireNonNull(event.getChannelLeft()).getId().equals(VoceChannelsID.WAITING_ROOM)) {
            if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(RoleIsID.DEBATER_APF) && timerEnd != 0)) {
                removeDebaterFromList(event.getMember().getUser());
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

    private void onDebaterSubscribeClick(@NotNull ButtonInteractionEvent event, User user) {
        if (event.getMember() != null) {
            if (event.getMember().getVoiceState() != null && event.getMember().getVoiceState().inAudioChannel() && Objects.equals(Objects.requireNonNull(event.getMember().getVoiceState().getChannel()).getId(), VoceChannelsID.WAITING_ROOM)) {
                if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(RoleIsID.DEBATER_APF))) {
                    addDebaterToDb(user);
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.DEBATER_ADDED));
                } else {
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_DEBATER_ROLE));
                }
            } else {
                showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_WAITING_ROOM));
            }
        }
    }

    private void onJudgeSubscribeClick(@NotNull ButtonInteractionEvent event, User user) {
        if (event.getMember() != null) {
            if (event.getMember().getVoiceState() != null && event.getMember().getVoiceState().inAudioChannel() && Objects.equals(Objects.requireNonNull(event.getMember().getVoiceState().getChannel()).getId(), VoceChannelsID.WAITING_ROOM)) {
                if (event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(RoleIsID.JUDGE_APF))) {
                    addJudgeToDb(user);
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.JUDGE_ADDED));
                } else {
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_JUDGE_ROLE));
                }
            } else {
                showEphemeralMessage(event, stringsRes.get(StringRes.Key.NEED_WAITING_ROOM));
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
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.DEBATER_REMOVED));
                } else if (judgeList != null && judgeList.contains(user.getAsMention())) {
                    removeJudgeFromList(user);
                    showEphemeralMessage(event, stringsRes.get(StringRes.Key.JUDGE_REMOVED));
                }
            }
            if (debatersList != null && debatersList.contains(user.getAsMention())) {
                removeDebaterFromList(user);
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

                List<String> debaters = debatersList.stream().map(User::getAsMention).toList();
                String debaterListString = debaters.isEmpty() ? stringsRes.get(StringRes.Key.NO_MEMBERS) : String.join("\n", debaters);
                embedBuilder.addField(stringsRes.get(StringRes.Key.DEBATER_LIST_TITLE), debaterListString, true);

                List<String> judges = judgesList.stream().map(User::getAsMention).toList();
                String judgeListString = judges.isEmpty() ? stringsRes.get(StringRes.Key.NO_MEMBERS) : String.join("\n", judges);
                embedBuilder.addField(stringsRes.get(StringRes.Key.JUDGES_LIST_TITLE), judgeListString, true);

                if (debaters.size() >= DEBATERS_LIMIT && judges.size() >= JUDGES_LIMIT && timerEnd == 0) {
                    timerEnd = System.currentTimeMillis() / 1000L + START_DEBATE_TIMER;
                    String timerMessage = "<t:" + timerEnd + ":R>";
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
            if (System.currentTimeMillis() / 1000L >= timerEnd && timerEnd != 0) {
                EmbedBuilder embedBuilder = new EmbedBuilder(message.getEmbeds().get(0));

                embedBuilder.getFields().stream().filter(field -> Objects.equals(field.getName(), stringsRes.get(StringRes.Key.TIMER_TITLE))).findFirst()
                        .ifPresent(field -> embedBuilder.addField(Objects.requireNonNull(field.getName()), stringsRes.get(StringRes.Key.DEBATE_STARTED), false));
                if (embedBuilder.getFields().size() > 2) {
                    embedBuilder.getFields().remove(2);
                } else {
                    System.out.println("Недостаточно полей для удаления.");
                }

                Button debaterButton = Button.success(DEBATER_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_DEBATER)).asDisabled();
                Button judgeButton = Button.primary(JUDGE_SUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_SUBSCRIBE_JUDGE)).asDisabled();
                Button unsubscribeButton = Button.danger(UNSUBSCRIBE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_UNSUBSCRIBE)).asDisabled();

                channel.editMessageEmbedsById(message.getId(), embedBuilder.build()).setActionRow(debaterButton, judgeButton, unsubscribeButton).queue();
                timerEnd = 0;

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
            List<String> channelNames = List.of("Судейская", "Трибуна", "Правительство", "Оппозиция");
            List<VoiceChannel> existingChannels = category.getVoiceChannels();

            for (String name : channelNames) {
                boolean isOpen = name.equals("Трибуна");

                for (VoiceChannel existingChannel : existingChannels) {
                    if (existingChannel.getName().equalsIgnoreCase(name)) {
                        existingChannel.delete().queue();
                        break;
                    }
                }

                category.createVoiceChannel(name)
                        .addPermissionOverride(everyoneRole, isOpen ? allowForTribune : allow, isOpen ? denyForTribune : deny)
                        .queue();
            }
        } else {
            System.out.println("Категория не найдена. Проверьте ID.");
        }
    }

    private void assignRoleToUser(List<User> debatersList, List<User> judgesList) {
        Collections.shuffle(debatersList);

        if (!debatersList.isEmpty()) {
            apiRepository.assignRoleToUser(debatersList.get(0).getId(), RoleIsID.HEAD_GOVERNMENT);
            if (debatersList.size() > 1) {
                apiRepository.assignRoleToUser(debatersList.get(1).getId(), RoleIsID.HEAD_OPPOSITION);
                for (int i = 2; i < debatersList.size(); i++) {
                    if (i % 2 == 0) {
                        apiRepository.assignRoleToUser(debatersList.get(i).getId(), RoleIsID.MEMBER_GOVERNMENT);
                    } else {
                        apiRepository.assignRoleToUser(debatersList.get(i).getId(), RoleIsID.MEMBER_OPPOSITION);
                    }
                }
            }
        }

        for (User user : judgesList) {
            apiRepository.assignRoleToUser(user.getId(), RoleIsID.JUDGE);
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

    private void showEphemeralMessage(@NotNull ButtonInteractionEvent event, String message) {
        if (!event.isAcknowledged()) {
            event.deferReply(true).queue(hook -> hook.sendMessage(message)
                    .queue(m -> m.delete().queueAfter(HELP_MESSAGE_TIME, TimeUnit.SECONDS)));
        } else {
            System.out.println("Интеракция уже была обработана.");
        }
    }

}
