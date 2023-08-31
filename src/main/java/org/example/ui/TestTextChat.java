package org.example.ui;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.example.constants.ButtonIds;
import org.example.constants.ChannelIds;
import org.example.constants.RoleIds;
import org.example.repository.ApiRepository;
import org.example.repository.DbRepository;
import org.example.repository.DebaterMapper;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;


public class TestTextChat implements TextChat {
    TextChannel RatingChannel;
    ApiRepository apiRepository;
    DbRepository dbRepository;
    Button testButton;
    Button currentBtn;
    List<Button> allButtonsList;

    public TestTextChat(ApiRepository apiRepository, DbRepository dbRepository) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        RatingChannel = apiRepository.getTextChannel(ChannelIds.TEST);

        testButton = Button.secondary(ButtonIds.TEAMS, "Кнопка");

        allButtonsList = List.of(testButton);

        RatingChannel.getHistoryFromBeginning(1).queue(history -> {
            if (history.isEmpty()) {
                RatingChannel.sendMessage("Начало")
                        .setActionRow(allButtonsList)
                        .queue();
            } else {
                Message firstMessage = history.getRetrievedHistory().get(0);
                firstMessage.editMessage("Чтото").queue();
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

        }
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        if (event.getRoles().stream().anyMatch(role -> role.getId().equals(RoleIds.DEBATERS))) {
            dbRepository.deleteDebater(DebaterMapper.mapFromMember(event.getMember()).getId());

        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.getChannel().getId().equals(ChannelIds.RATING)) return;

    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        currentBtn = event.getButton();
        String currentBtnId = Objects.requireNonNull(currentBtn.getId());
        if (currentBtnId.equals(testButton.getId())) onClickDebatersBtn(event);

    }


///////////////////////////////////////////////////////////////////////////////////////////


    private void onClickDebatersBtn(@NotNull ButtonInteractionEvent event) {
//        event.deferEdit().queue();
        ReplyCallbackAction action = event.reply("Изначальное сообщение");
        action.setEphemeral(true);
        action.queue(message -> {
            message.editOriginal("Отредактированное сообщение").queue();
        });

    }


}
