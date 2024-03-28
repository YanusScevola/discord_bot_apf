package org.example.core.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.core.constants.TextChannelsID;
import org.example.domain.UseCase;

import java.awt.*;
import java.util.Timer;

public class Utils {

    public static void sendLogDebug(UseCase useCase, String title, String description) {
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

    public static void sendLogError(UseCase useCase, String title, String description) {
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

    public static void startTimer(int seconds, TimerCallBack callBack) {
        Timer timer = new Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                callBack.onTimer();
            }
        }, seconds * 1000L);
    }

    public interface TimerCallBack {
        void onTimer();
    }




}
