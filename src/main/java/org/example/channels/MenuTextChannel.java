package org.example.channels;

import java.awt.*;
import java.util.*;
import java.util.List;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
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


public class MenuTextChannel implements  MessageEventListener, RoleEventListener,SelectorEventListener {
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
                                .addOption("По количеству баллов", BALLS_SELECT_ID, Emoji.fromUnicode("\uD83D\uDCAF"))
                                .build()).queue();
            }
        });

        updateDebatersDB();


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
        String nicknamesRow = getListDebatersText(sortedDebaterList, property);

        String title = switch (property) {
            case WINNER -> "количеству побед";
            case DEBATE_COUNT -> "количеству сыгранных дебатов";
            case BALLS -> "количеству баллов";
        };

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Рейтинг по " + title, null);
        eb.setColor(new Color(0x2F51B9));
        eb.setDescription(nicknamesRow);

        interactionHook.sendMessage("").setEmbeds(eb.build()).queue();
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

    public String getListDebatersText(List<Debater> filteredMembers, SortTypes property) {
        StringBuilder clickableNicknames = new StringBuilder();

        int lineNumber = 1;
        for (Debater debater : filteredMembers) {
            if (debater != null) {
                int value = switch (property) {
                    case WINNER -> debater.getWinner();
                    case DEBATE_COUNT -> debater.getDebateCount();
                    case BALLS -> debater.getBalls();
                };

                String medal = switch (lineNumber) {
                    case 1 -> "\uD83E\uDD47";
                    case 2 -> "\uD83E\uDD48";
                    case 3 -> "\uD83E\uDD49";
                    default -> "";
                };

                clickableNicknames.append(lineNumber).append(". ");
                clickableNicknames.append(medal).append("<@").append(debater.getId()).append(">|  **");
                clickableNicknames.append(value).append("**");
                clickableNicknames.append("\n");
                lineNumber++;
            }
        }

        String limitedString = clickableNicknames.toString();
        limitedString = limitedString.substring(0, Math.min(limitedString.length(), 2000));

        return limitedString;
    }

    private void updateDebatersDB() {
        apiRepository.getMembersByRole(RoleIds.DEBATERS).thenAccept(members -> {
            List<Debater> debaterList = new ArrayList<>(DebaterMapper.mapFromMembers(members));
            dbRepository.insertDebaters(debaterList);
        });

    }


}
