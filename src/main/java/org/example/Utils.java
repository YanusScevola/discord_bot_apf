package org.example;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.example.models.Debater;
import org.example.ui.InfoTextChat;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Utils {

//    public static String getRows(List<Debater> filteredMembers) {
//        StringBuilder clickableNicknames = new StringBuilder();
//
//        clickableNicknames.append(" **Статистика:** победы/количество дебатов.\n");
//
//        int lineNumber = 1;
//        for (Debater debater : filteredMembers) {
//            if (debater != null) {
//                clickableNicknames.append(lineNumber).append(". ");
//                clickableNicknames.append("<@").append(debater.getId()).append("> - **");
//                clickableNicknames.append(debater.getDebateCount()).append("**");
////                clickableNicknames.append(debater.getDebateCount()).append("** ");
////                clickableNicknames.append(debater.getBalls()).append(" \n");
//                clickableNicknames.append("\n");
//
//                lineNumber++;
//            }
//        }
//
//        String limitedString = clickableNicknames.toString();
//        limitedString = limitedString.substring(0, Math.min(limitedString.length(), 2000));
//
//        return limitedString;
//    }


    public static void showButtons(@NotNull TextChannel botTextChannel, List<Button> buttons) {
        botTextChannel.sendMessage(" ")
                .setActionRow(buttons)
                .queue();
    }


    public static void hideButtons(Message message, Button debatersBtn) {
//            event.getMessage().editMessageComponents().queue();
        Button disabledButton = debatersBtn.asDisabled();
        List<ActionRow> rows = List.of(ActionRow.of(disabledButton));
        message.editMessageComponents(rows).queue();
    }


    public static List<Debater> sortDebaters(List<Debater> debaters, InfoTextChat.DebaterProperty property) {
        Collections.sort(debaters, new Comparator<Debater>() {
            @Override
            public int compare(Debater o1, Debater o2) {
                int result = 0;

                switch (property) {
                    case WINNER:
                        result = Integer.compare(o2.getWinner(), o1.getWinner());
                        break;
                    case DEBATE_COUNT:
                        result = Integer.compare(o2.getDebateCount(), o1.getDebateCount());
                        break;
                    case BALLS:
                        result = Integer.compare(o2.getBalls(), o1.getBalls());
                        break;
                    default:
                        break;
                }

                return result;
            }
        });

        return debaters;
    }


}
