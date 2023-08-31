package org.example.ui;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.example.constants.ButtonIds;
import org.example.constants.ChannelIds;
import org.example.constants.MessageIds;
import org.example.constants.RoleIds;
import org.example.models.Debater;
import org.example.repository.ApiRepository;
import org.example.repository.DbRepository;
import org.example.repository.DebaterMapper;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;


public class RatingTextChat implements TextChat {
    static TextChannel channel;

    public enum DebaterProperty {DEBATE_COUNT, WINNER, BALLS}

    DebaterProperty debaterProperty = DebaterProperty.WINNER;
    ApiRepository apiRepository;
    DbRepository dbRepository;
    Button debatersBtn;
    Button winnsBtn;
    Button ballsBtn;
    Button currentBtn;
    List<Button> allButtonsList;

    public RatingTextChat(ApiRepository apiRepository, DbRepository dbRepository) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        if (channel == null) channel = apiRepository.getTextChannel(ChannelIds.RATING);

        winnsBtn = Button.secondary(ButtonIds.TEAMS, "Победы");
        debatersBtn = Button.secondary(ButtonIds.DEBATERS, "Дебаты");
        ballsBtn = Button.secondary(ButtonIds.MUSIC, "Баллы");

        allButtonsList = List.of(winnsBtn, debatersBtn, ballsBtn);

        channel.getHistoryFromBeginning(1).queue(history -> {
            if (history.isEmpty()) {
                List<Debater> debaterList = dbRepository.getAllDebaters();
                List<Debater> sortedDebaterList = sortDebaters(debaterList, DebaterProperty.WINNER);
                String nicknamesRow = getRowsDebaters(sortedDebaterList, DebaterProperty.WINNER);
                channel.sendMessage(nicknamesRow)
                        .setActionRow(allButtonsList)
                        .queue();
            } else {
                Message firstMessage = history.getRetrievedHistory().get(0);
                updateDebatersListView(firstMessage, debaterProperty);
            }
        });

        apiRepository.getMembersByRole(RoleIds.DEBATERS)
                .thenAccept(members -> dbRepository.insertDebaters(DebaterMapper.mapFromMembers(members)))
                .exceptionally(e -> null);


    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {

    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        if (event.getRoles().stream().anyMatch(role -> role.getId().equals(RoleIds.DEBATERS))) {
            dbRepository.insertDebater(DebaterMapper.mapFromMember(event.getMember()));
            channel.retrieveMessageById(MessageIds.INFORMATION).queue(debaters ->
                    updateDebatersListView(debaters, debaterProperty)
            );
        }
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        if (event.getRoles().stream().anyMatch(role -> role.getId().equals(RoleIds.DEBATERS))) {
            dbRepository.deleteDebater(DebaterMapper.mapFromMember(event.getMember()).getId());
            channel.retrieveMessageById(MessageIds.INFORMATION).queue(debaters ->
                    updateDebatersListView(debaters, debaterProperty)
            );
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.getChannel().getId().equals(ChannelIds.RATING)) return;

//        String message = event.getMessage().getContentDisplay();
//        updateDebatersListView(event.getMessage());

//        if (message.equals("!start")) {
//        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        currentBtn = event.getButton();
        String currentBtnId = Objects.requireNonNull(currentBtn.getId());
        if (currentBtnId.equals(debatersBtn.getId())) onClickDebatersBtn(event);
        if (currentBtnId.equals(winnsBtn.getId())) onClickWinnerBtn(event);
        if (currentBtnId.equals(ballsBtn.getId())) onClickBallsBtn(event);

    }


///////////////////////////////////////////////////////////////////////////////////////////


    private void onClickDebatersBtn(@NotNull ButtonInteractionEvent event) {
        event.deferEdit().queue();
        debaterProperty = DebaterProperty.DEBATE_COUNT;
        updateDebatersListView(event.getMessage(), debaterProperty);
    }

    private void onClickWinnerBtn(@NotNull ButtonInteractionEvent event) {
        event.deferEdit().queue();
        debaterProperty = DebaterProperty.WINNER;
        updateDebatersListView(event.getMessage(), debaterProperty);
    }

    private void onClickBallsBtn(@NotNull ButtonInteractionEvent event) {
        event.deferEdit().queue();
        debaterProperty = DebaterProperty.BALLS;
        updateDebatersListView(event.getMessage(), debaterProperty);
    }

    private void updateDebatersListView(@NotNull Message message, DebaterProperty property) {
        List<Debater> debaterList = dbRepository.getAllDebaters();
        List<Debater> sortedDebaterList = sortDebaters(debaterList, property);
        String nicknamesRow = getRowsDebaters(sortedDebaterList, property);
        message.editMessage(nicknamesRow).queue();
    }

    public String getRowsDebaters(List<Debater> filteredMembers, DebaterProperty property) {
        StringBuilder clickableNicknames = new StringBuilder();
        String title = "";

        title = switch (property) {
            case WINNER -> "количеству побед";
            case DEBATE_COUNT -> "количеству сыгранных дебатов";
            case BALLS -> "количеству баллов";
        };
        clickableNicknames.append("## Рейтинг по ").append(title).append(": \n");

        int lineNumber = 1;
        for (Debater debater : filteredMembers) {
            if (debater != null) {
                int value = switch (property) {
                    case WINNER -> debater.getWinner();
                    case DEBATE_COUNT -> debater.getDebateCount();
                    case BALLS -> debater.getBalls();
                };
                clickableNicknames.append(lineNumber).append(". ");
                clickableNicknames.append("<@").append(debater.getId()).append("> - **");
                clickableNicknames.append(value).append("**");
                clickableNicknames.append("\n");
                lineNumber++;
            }
        }

        String limitedString = clickableNicknames.toString();
        limitedString = limitedString.substring(0, Math.min(limitedString.length(), 2000));

        return limitedString;
    }


    public List<Debater> sortDebaters(List<Debater> debaters, RatingTextChat.DebaterProperty property) {
        Collections.sort(debaters, new Comparator<Debater>() {
            @Override
            public int compare(Debater o1, Debater o2) {
                int result = 0;

                switch (property) {
                    case WINNER -> result = Integer.compare(o2.getWinner(), o1.getWinner());
                    case DEBATE_COUNT -> result = Integer.compare(o2.getDebateCount(), o1.getDebateCount());
                    case BALLS -> result = Integer.compare(o2.getBalls(), o1.getBalls());
                    default -> {
                    }
                }

                return result;
            }
        });

        return debaters;
    }


}
