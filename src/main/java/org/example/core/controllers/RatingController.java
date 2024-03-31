package org.example.core.controllers;

import java.awt.*;
import java.util.Comparator;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.core.constants.TextChannelsID;
import org.example.core.constants.RolesID;
import org.example.core.models.Debater;
import org.example.domain.UseCase;

public class RatingController {
    private static RatingController instance;
    private TextChannel channel;
    private UseCase useCase;

    private RatingController(UseCase useCase) {
        this.useCase = useCase;
        this.channel = useCase.getTextChannel(TextChannelsID.RATING);
    }

    public static synchronized RatingController getInstance(UseCase useCase) {
        if (instance == null) {
            instance = new RatingController(useCase);
        }
        return instance;
    }

    public void displayDebatersList() {
        try {
            this.channel = useCase.getTextChannel(TextChannelsID.RATING);
            useCase.getMembersByRole(RolesID.DEBATER_APF).thenAccept(members -> {
                var membersIds = members.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
                useCase.getDebatersByMemberId(membersIds).thenAccept(debaters -> {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Рейтинг дебатеров АПФ", null);
                    eb.setColor(new Color(88, 100, 242));
                    eb.setDescription("Список дебатеров");

                    debaters.sort(Comparator.comparing(Debater::getWinnCount).reversed());
                    var limitedDebaters = debaters.stream().limit(20).toList();

                    int debaterRatingNumber = 1;
                    StringBuilder listDebaters = new StringBuilder();
                    StringBuilder listWins = new StringBuilder();
                    StringBuilder listLosses = new StringBuilder();

                    for (int i = 0; i < 5; i++) {
                        Debater debater = limitedDebaters.get(0);
//                for (Debater debater : limitedDebaters) {
                        if (debaterRatingNumber == 1) {
                            listDebaters.append("1" + " : ").append("<@").append(debater.getMemberId()).append(">\n");
                        }else {
                            listDebaters.append(debaterRatingNumber).append(": ").append("<@").append(debater.getMemberId()).append(">\n");
                        }

//                        if (debaterRatingNumber == 1) {
//                            listDebaters.append(":first_place:" + ": ").append("<@").append(debater.getMemberId()).append(">\n");
//                        } else if (debaterRatingNumber == 2) {
//                            listDebaters.append(":second_place:" + ": ").append("<@").append(debater.getMemberId()).append(">\n");
//                        } else if (debaterRatingNumber == 3) {
//                            listDebaters.append(":third_place:" + ": ").append("<@").append(debater.getMemberId()).append(">\n");
//                        } else {
//                            listDebaters.append(" ").append(debaterRatingNumber).append(" : ").append("<@").append(debater.getMemberId()).append(">\n");
//                        }


                        listWins.append("").append(debater.getWinnCount()).append("\n");
                        listLosses.append("").append(debater.getLossesCount()).append("\n");
                        debaterRatingNumber++;
                    }

                    eb.addField("Дебатеры", listDebaters.toString(), true);
                    eb.addField("Победы", listWins.toString(), true);
                    eb.addField("Поражения", listLosses.toString(), true);

                    channel.getHistoryFromBeginning(1).queue(history -> {
                        if (!history.getRetrievedHistory().isEmpty()) {
                            Message existingMessage = history.getRetrievedHistory().get(0);
                            channel.editMessageEmbedsById(existingMessage.getId(), eb.build()).queue();
                        } else {
                            channel.sendMessageEmbeds(eb.build()).queue();
                        }
                    });
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
