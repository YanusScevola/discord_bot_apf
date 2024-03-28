package org.example.core.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.core.constants.TextChannelsID;
import org.example.domain.UseCase;

import java.awt.*;

public class Log {

    public static void sendDebug(UseCase useCase, String title, String description) {
        TextChannel channel = useCase.getTextChannel(TextChannelsID.LOG);
        if (channel != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(title);
            eb.setDescription(description);
            eb.setColor(Color.BLUE);

            channel.sendMessageEmbeds(eb.build()).queue();
        } else {
            System.out.println("Текстовый канал не найден: " + TextChannelsID.LOG);
        }
    }

    public static void sendError(UseCase useCase, String title, String description) {
        TextChannel channel = useCase.getTextChannel(TextChannelsID.LOG);
        if (channel != null) {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(title);
            eb.setDescription(description);
            eb.setColor(Color.RED);

            channel.sendMessageEmbeds(eb.build()).queue();
        } else {
            System.out.println("Текстовый канал не найден: " + TextChannelsID.LOG);
        }
    }
}
