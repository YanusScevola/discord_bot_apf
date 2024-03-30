package org.example.core;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {
    public static void main(String[] args) {
        JDA jda;

        try {
//            String token = new Properties().getProperty("token");
            String token = "MTE5MDM5NDQ1NDg0NzI3OTE4NA.GlZsAO.a-Iqst8WZQBYTlEckvSAJqYW5enveezx0Cnlg0";

//            DataBase2.getInstance();

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