package org.example.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.data.repository.ApiRepository;
import org.example.core.constants.TextChannelsID;

import java.awt.*;

public class Log {

    public static void sendDebug(ApiRepository repository, String title, String description) {
        TextChannel channel = repository.getTextChannel(TextChannelsID.LOG);
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

    public static void sendError(ApiRepository repository, String title, String description) {
        TextChannel channel = repository.getTextChannel(TextChannelsID.LOG);
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
