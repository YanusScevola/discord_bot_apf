package org.example.core.controllers;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.core.constants.TextChannelsID;
import org.example.core.constants.RolesID;
import org.example.core.models.DebaterAPF;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.utils.DebaterMapper;

public class RatingController {
    TextChannel channel;
    ApiRepository apiRepository;
    DbRepository dbRepository;


    public RatingController(ApiRepository apiRepository, DbRepository dbRepository) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        this.channel = apiRepository.getTextChannel(TextChannelsID.RATING);

        displayDebatersList();
        updateDebatersDB();
    }

    private void displayDebatersList() {
        List<DebaterAPF> debaterAPFList = dbRepository.getAllDebaters();
//        debaterAPFList.sort((o1, o2) -> Integer.compare(o2.getWinner(), o1.getWinner()));
        String debatersText = getListDebatersText(debaterAPFList);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Рейтинг по победам в АПФ", null);
        eb.setColor(new Color(0x2F51B9));
        eb.setDescription(debatersText);

        channel.getHistoryFromBeginning(1).queue(history -> {
            if (!history.getRetrievedHistory().isEmpty()) {
                Message existingMessage = history.getRetrievedHistory().get(0);
                channel.editMessageEmbedsById(existingMessage.getId(), eb.build()).queue();
            } else {
                channel.sendMessageEmbeds(eb.build()).queue();
            }
        });
    }

    public String getListDebatersText(List<DebaterAPF> filteredMembers) {
        StringBuilder clickableNicknames = new StringBuilder();

        int lineNumber = 1;
        for (DebaterAPF debaterAPF : filteredMembers) {
            if (lineNumber > 20) break;
            if (debaterAPF != null) {
//                int value =  debaterAPF.getWinner();
                String medal = switch (lineNumber) {
                    case 1 -> "\uD83E\uDD47";
                    case 2 -> "\uD83E\uDD48";
                    case 3 -> "\uD83E\uDD49";
                    default -> "";
                };

                clickableNicknames.append(lineNumber).append(". ");
                clickableNicknames.append(medal).append(" <@").append(debaterAPF.getMemberId()).append(">|  **");
//                clickableNicknames.append(value).append("**");
                clickableNicknames.append("\n");
                lineNumber++;
            }
        }

        String limitedString = clickableNicknames.toString();
        limitedString = limitedString.substring(0, Math.min(limitedString.length(), 2000));
        return limitedString;
    }


    private void updateDebatersDB() {
        apiRepository.getMembersByRole(RolesID.DEBATER_APF).thenAccept(members -> {
            List<DebaterAPF> debaterAPFList = new ArrayList<>(DebaterMapper.mapFromMembers(members));
            dbRepository.insertDebaters(debaterAPFList);
        });
    }


}
