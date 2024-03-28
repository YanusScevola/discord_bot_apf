package org.example.core.controllers;

import java.awt.*;
import java.util.Comparator;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.core.constants.TextChannelsID;
import org.example.core.constants.RolesID;
import org.example.core.models.Debater;
import org.example.domain.UseCase;

public class RatingController {
    TextChannel channel;
    UseCase useCase;


    public RatingController(UseCase useCase) {
        this.useCase = useCase;
        this.channel = useCase.getTextChannel(TextChannelsID.RATING);

        displayDebatersList();
        updateDebatersDB();
    }

    private void displayDebatersList() {
        useCase.getAllDebaters().thenAccept(debaters -> {
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Рейтинг по победам в АПФ", null);
            eb.setColor(new Color(0x2F51B9));
            eb.setDescription("Список дебатеров");

            // Сортировка дебатеров по количеству побед (можно адаптировать под вашу логику)
            debaters.sort(Comparator.comparing(Debater::getWinnDebatesCount).reversed());

            for (Debater debater : debaters) {
                // Форматируем строку с никнеймом и результатами
                String debaterInfo = String.format("%s: %d/%d", debater.getNickname(), debater.getWinnDebatesCount(), debater.getLossesDebatesCount());

                // Добавляем информацию как поле с параметром inline, чтобы расположить данные в две колонки
                eb.addField("Дебатер", debater.getNickname(), true);
                eb.addField("Победы/Поражения", String.format("%d/%d", debater.getWinnDebatesCount(), debater.getLossesDebatesCount()), true);
            }

            channel.getHistoryFromBeginning(1).queue(history -> {
                if (!history.getRetrievedHistory().isEmpty()) {
                    Message existingMessage = history.getRetrievedHistory().get(0);
                    channel.editMessageEmbedsById(existingMessage.getId(), eb.build()).queue();
                } else {
                    channel.sendMessageEmbeds(eb.build()).queue();
                }
            });
        });
    }


    private void updateDebatersDB() {
        useCase.getMembersByRole(RolesID.DEBATER_APF).thenAccept(members -> {

        });
    }


}
