package org.example.channels;

import java.util.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.example.constants.ChannelIds;
import org.example.constants.RoleIds;
import org.example.interfaces.*;
import org.example.models.Debater;
import org.example.repository.ApiRepository;
import org.example.repository.DbRepository;
import org.example.DebaterMapper;
import org.jetbrains.annotations.NotNull;
import static org.example.channels.MenuTextChannel.SortTypes.*;


public class MenuTextChannel implements  MessageEventListener, RoleEventListener, MemberEventListener, SelectorEventListener {
    static TextChannel channel;

    public enum SortTypes {DEBATE_COUNT, WINNER, BALLS}

    ApiRepository apiRepository;
    DbRepository dbRepository;

    SortTypes currentSortType = WINNER;
    InteractionHook currentInteractionHook;


    public static final String WINNER_SELECT_ID = "winner";
    public static final String DEBATE_COUNT_SELECT_ID = "debate_count";
    public static final String BALLS_SELECT_ID = "balls";


    public MenuTextChannel(ApiRepository apiRepository, DbRepository dbRepository) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        if (channel == null) channel = apiRepository.getTextChannel(ChannelIds.RATING);


        channel.getHistoryFromBeginning(1).queue(history -> {
            if (history.isEmpty()) {
                channel.sendMessage("**Рейтинг учасников клуба**")
                        .setActionRow(StringSelectMenu.create("statistic")
                                .addOption("По количеству побед", WINNER_SELECT_ID, "", Emoji.fromUnicode("\uD83E\uDD47"))
                                .addOption("По количеству дебатов", DEBATE_COUNT_SELECT_ID, Emoji.fromUnicode("\uD83D\uDDE3️"))
                                .addOption("По количеству баллов", BALLS_SELECT_ID, Emoji.fromUnicode("\uD83D\uDC8E"))
                                .build()).queue();
            }
        });

        updateDebatersDB();


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
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("statistic")) {
            switch (event.getSelectedOptions().get(0).getValue()) {
                case WINNER_SELECT_ID -> currentSortType = WINNER;
                case DEBATE_COUNT_SELECT_ID -> currentSortType = DEBATE_COUNT;
                case BALLS_SELECT_ID -> currentSortType = BALLS;
            }

            event.deferReply(true).setEphemeral(true).queue(interactionHook -> {
                currentInteractionHook = interactionHook;
                openDebatersListView(currentSortType, currentInteractionHook);
            });

        }
    }


    private void openDebatersListView(SortTypes property, InteractionHook interactionHook) {
        List<Debater> debaterUpdatedList = dbRepository.getAllDebaters();
        List<Debater> sortedDebaterList = sortDebaters(debaterUpdatedList, property);
        String nicknamesRow = getTextListDebaters(sortedDebaterList, property);
        interactionHook.sendMessage(nicknamesRow).queue();
    }


    public List<Debater> sortDebaters(List<Debater> debaters, SortTypes property) {
        Collections.sort(debaters, (o1, o2) -> {
            int result = 0;

            switch (property) {
                case WINNER -> result = Integer.compare(o2.getWinner(), o1.getWinner());
                case DEBATE_COUNT -> result = Integer.compare(o2.getDebateCount(), o1.getDebateCount());
                case BALLS -> result = Integer.compare(o2.getBalls(), o1.getBalls());
                default -> {
                }
            }
            return result;
        });

        return debaters;
    }


    public String getTextListDebaters(List<Debater> filteredMembers, SortTypes property) {
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

    public List<Button> updateButtons(List<Button> originalButtons, Button currentBtn) {
        List<Button> modifiedButtons = new ArrayList<>();
        for (Button button : originalButtons) {
            if (Objects.equals(button.getId(), currentBtn.getId())) {
                modifiedButtons.add(button.asDisabled());
            } else {
                modifiedButtons.add(button);
            }
        }
        return modifiedButtons;
    }


    public List<ActionRow> createActionRowList(List<Button> buttons) {
        List<ActionRow> rows = new ArrayList<>();
        List<Button> tempRow = new ArrayList<>();

        for (Button button : buttons) {
            if (tempRow.size() < 4) {
                tempRow.add(button);
            } else {
                rows.add(ActionRow.of(tempRow));
                tempRow.clear();
                tempRow.add(button);
            }
        }

        if (!tempRow.isEmpty()) {
            rows.add(ActionRow.of(tempRow));
        }

        return rows;
    }

    private void updateDebatersDB() {
        apiRepository.getMembersByRole(RoleIds.DEBATERS).thenAccept(members -> {
            List<Debater> debaterList = new ArrayList<>(DebaterMapper.mapFromMembers(members));
            dbRepository.insertDebaters(debaterList);
        });

    }


}
