package org.example.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.data.repository.ApiRepository;
import org.example.ui.constants.TextChannelsID;

import java.awt.*;
import java.util.Timer;

public class Utils {

    public static void sendLogDebug(ApiRepository repository, String title, String description) {
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

    public static void sendLogError(ApiRepository repository, String title, String description) {
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
