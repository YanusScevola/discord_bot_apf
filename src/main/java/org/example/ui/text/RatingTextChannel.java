package org.example.ui.text;

import java.awt.*;
import java.util.*;
import java.util.List;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.ui.constants.TextChannelsID;
import org.example.ui.constants.RoleIsID;
import org.example.ui.models.Debater;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.utils.DebaterMapper;



public class RatingTextChannel  {
    static TextChannel channel;
    ApiRepository apiRepository;
    DbRepository dbRepository;


    public RatingTextChannel(ApiRepository apiRepository, DbRepository dbRepository) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        if (channel == null) channel = apiRepository.getTextChannel(TextChannelsID.RATING);


        channel.getHistoryFromBeginning(1).queue(history -> {
            if (history.isEmpty()) {
                displayDebatersList();
            }
        });

        updateDebatersDB();

    }

    private void displayDebatersList() {
        List<Debater> debaterList = dbRepository.getAllDebaters();
        debaterList.sort((o1, o2) -> Integer.compare(o2.getWinner(), o1.getWinner()));
        String debatersText = getListDebatersText(debaterList);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Рейтинг по победам в АПФ", null);
        eb.setColor(new Color(0x2F51B9));
        eb.setDescription(debatersText);

        channel.sendMessageEmbeds(eb.build()).queue();
    }

    public String getListDebatersText(List<Debater> filteredMembers) {
        StringBuilder clickableNicknames = new StringBuilder();

        int lineNumber = 1;
        for (Debater debater : filteredMembers) {
            if (debater != null) {
                int value =  debater.getWinner();
                String medal = switch (lineNumber) {
                    case 1 -> "\uD83E\uDD47";
                    case 2 -> "\uD83E\uDD48";
                    case 3 -> "\uD83E\uDD49";
                    default -> "";
                };

                clickableNicknames.append(lineNumber).append(". ");
                clickableNicknames.append(medal).append(" <@").append(debater.getId()).append(">|  **");
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
        apiRepository.getMembersByRole(RoleIsID.DEBATER).thenAccept(members -> {
            List<Debater> debaterList = new ArrayList<>(DebaterMapper.mapFromMembers(members));
            dbRepository.insertDebaters(debaterList);
        });

    }


}
