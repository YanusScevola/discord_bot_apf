package org.example.core.controllers;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.core.constants.TextChannelsID;
import org.example.domain.UseCase;

import java.awt.*;

public class HistoryController {
    private static HistoryController instance;
    private final TextChannel channel;
    private final UseCase useCase;

    private HistoryController(UseCase useCase) {
        this.useCase = useCase;
        this.channel = useCase.getTextChannel(TextChannelsID.HISTORY);
    }

    public static synchronized HistoryController getInstance(UseCase useCase) {
        if (instance == null) {
            instance = new HistoryController(useCase);
        }
        return instance;
    }

    public void sendDebateTopicMessage() {
        useCase.getLastDebate().thenAccept(lastDebate -> {
            EmbedBuilder embed = new EmbedBuilder();

            StringBuilder governmentDebatersString = new StringBuilder();
            StringBuilder oppositionDebatersString = new StringBuilder();

            String iconForGovernment = lastDebate.isGovernmentWinner() ? " ㅤ  ㅤ :trophy:\n" : ": ㅤ  ㅤ baby_bottle:\n";
            String iconForOpposition = lastDebate.isGovernmentWinner() ? " ㅤ  ㅤ :baby_bottle:\n" : " ㅤ  ㅤ :trophy:\n";

            var governmentDebaters = lastDebate.getGovernmentDebaters();
            for (Member member : governmentDebaters) {
                governmentDebatersString.append("<@").append(member.getIdLong()).append(">\n");
            }

            var oppositionDebaters = lastDebate.getOppositionDebaters();
            for (Member member : oppositionDebaters) {
                oppositionDebatersString.append("<@").append(member.getIdLong()).append(">\n");
            }

            embed.setColor(new Color(88, 100, 242));
            embed.setTitle("Тема: " + getShortString(lastDebate.getTheme().getName()) + "\n ㅤ ");
            embed.addField(iconForGovernment + "Правительство", governmentDebatersString.toString(), true);
            embed.addField(iconForOpposition + "Оппозиция", oppositionDebatersString.toString(), true);

            if (this.channel != null) {
                this.channel.sendMessageEmbeds(embed.build()).queue();
            } else {
                System.err.println("Ошибка: канал не найден.");
            }
        });

    }

    public String getShortString(String input) {
        String[] words = input.split("\\s+");
        StringBuilder result = new StringBuilder();
        int wordCount = 0;

        for (int i = 0; i < words.length; i++) {
            if (words[i].length() > 2) {
                wordCount++;
                result.append(words[i]);

                if (wordCount % 3 == 0 && i < words.length - 1) {
                    result.append("\n");
                } else if (i < words.length - 1) {
                    result.append(" ");
                }
            } else {
                result.append(words[i]);
                if (i < words.length - 1) {
                    result.append(" ");
                }
            }
        }

        return result.toString();
    }



}
