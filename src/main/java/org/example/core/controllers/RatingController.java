package org.example.core.controllers;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.core.constants.TextChannelsID;
import org.example.core.constants.RolesID;
import org.example.core.models.Debater;
import org.example.domain.UseCase;
import org.example.resources.Colors;

public class RatingController {
    private static RatingController instance;
    private TextChannel channel;
    private UseCase useCase;
    private List<Long> debatersRuleIds = new ArrayList<>(Arrays.asList(
            RolesID.DEBATER_APF_1,
            RolesID.DEBATER_APF_2,
            RolesID.DEBATER_APF_3,
            RolesID.DEBATER_APF_4,
            RolesID.DEBATER_APF_5
    ));


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
            useCase.getMembersByRoles(debatersRuleIds).thenAccept(members -> {
                List<Long> membersIds = members.stream().map(ISnowflake::getIdLong).collect(Collectors.toList());
                useCase.getDebatersByMemberId(membersIds).thenAccept(debaters -> {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Рейтинг дебатеров АПФ", null);
                    eb.setColor(Colors.BLUE);

                    debaters.sort((debater1, debater2) -> {
                        double debater1WinRate = (double) debater1.getWinnCount() / (debater1.getWinnCount() + debater1.getLossesCount());
                        double debater2WinRate = (double) debater2.getWinnCount() / (debater2.getWinnCount() + debater2.getLossesCount());

                        int rateCompare = Double.compare(debater2WinRate, debater1WinRate);
                        if (rateCompare != 0) return rateCompare;
                        return Integer.compare(debater2.getWinnCount(), debater1.getWinnCount());
                    });

                    List<Debater> limitedDebaters = debaters.stream().limit(20).collect(Collectors.toList());
                    int debaterRatingNumber = 1;
                    StringBuilder listDebaters = new StringBuilder();
                    StringBuilder listWins = new StringBuilder();
                    StringBuilder listLosses = new StringBuilder();

                    for (Debater debater : limitedDebaters) {
                        if (debaterRatingNumber == 1) {
                            listDebaters.append("1" + ". ").append("<@").append(debater.getMemberId()).append(">\n");
                        } else {
                            listDebaters.append(debaterRatingNumber).append(". ").append("<@").append(debater.getMemberId()).append(">\n");
                        }

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
