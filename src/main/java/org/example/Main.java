package org.example;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.example.ui.MainListenerAdapter;

public class Main {
    public static void main(String[] args) {
        JDA jda;
        String token = "MTE5MDM5NDQ1NDg0NzI3OTE4NA.GZfcTy.2cuQfYZNIcTs457wAnGBN6cI6x1IiwUPP30IJ0";

        try {
//            Database.getInstance();

            jda = JDABuilder.createDefault(token)
                    .setActivity(Activity.playing("Дебаты"))
                    .setStatus(OnlineStatus.ONLINE)
                    .addEventListeners(new MainListenerAdapter())
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