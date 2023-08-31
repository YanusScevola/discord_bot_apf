package org.example;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.example.source.Database;
import org.example.ui.MyListenerAdapter;

public class Main {
    public static void main(String[] args) {
        JDA jda;
        String token = "MTE0NTA1OTk3MDQ3NjQyMTI0MA.GCoRjD.7EwItC8N_KZ52byF7b1mNTZDRQfricZRpsUyuo";

        try {
            Database.getInstance();

            jda = JDABuilder.createDefault(token)
                    .setActivity(Activity.playing("!help"))
                    .setStatus(OnlineStatus.ONLINE)
                    .addEventListeners(new MyListenerAdapter())
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES)
                    .enableIntents(GatewayIntent.DIRECT_MESSAGES)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                    .build();
            jda.awaitReady();



        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

    }

}