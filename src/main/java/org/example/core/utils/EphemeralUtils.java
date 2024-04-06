package org.example.core.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.example.core.models.TestDataByUser;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.TreeMap;

public class EphemeralUtils {

    public static void showTestFailed(ButtonInteractionEvent event, LinkedHashMap<Long, Long> lastTestAttemptByUser, LinkedHashMap<Member, TestDataByUser> testDataByUserMap, int MAX_QUESTIONS, String CLOSE_TEST_ID) {
        TestDataByUser currentTestData = testDataByUserMap.get(event.getMember());
        EmbedBuilder lossEmbed = new EmbedBuilder();
        lossEmbed.setColor(new Color(158, 26, 26));
        lossEmbed.setTitle("Тест провален :cry:");
        lossEmbed.setDescription("- Вы ответили правильно на " + (currentTestData.getCurrentQuestionNumber() - 1) + " из " + MAX_QUESTIONS + " вопросов.\n" +
                "- Перепройди тест через 10 минут.");

        event.editMessageEmbeds(lossEmbed.build()).setActionRow(net.dv8tion.jda.api.interactions.components.buttons.Button.danger(CLOSE_TEST_ID, "Закрыть")).queue(
                success -> {
                    lastTestAttemptByUser.put(Objects.requireNonNull(event.getMember()).getIdLong(), System.currentTimeMillis());
                    testDataByUserMap.remove(event.getMember());
                    System.out.println("2Сообщение изменено");
                },
                failure -> System.err.println("2Не удалось изменить оригинальное сообщение: " + failure.getMessage()));
    }

    public static void showTestFailed(Member member, InteractionHook hook, LinkedHashMap<Long, Long> lastTestAttemptByUser, LinkedHashMap<Member, TestDataByUser> testDataByUserMap, int MAX_QUESTIONS, String CLOSE_TEST_ID) {
        TestDataByUser currentTestData = testDataByUserMap.get(member);
        EmbedBuilder lossEmbed = new EmbedBuilder();
        lossEmbed.setColor(new Color(158, 26, 26));
        lossEmbed.setTitle("Тест провален :cry:");
        lossEmbed.setDescription("- Вы ответили правильно на " + (currentTestData.getCurrentQuestionNumber() - 1) + " из " + MAX_QUESTIONS + " вопросов.\n" +
                "- Перепройди тест через 10 минут.");

        hook.editOriginalEmbeds(lossEmbed.build()).setActionRow(net.dv8tion.jda.api.interactions.components.buttons.Button.danger(CLOSE_TEST_ID, "Закрыть")).queue(
                success -> {
                    lastTestAttemptByUser.put(Objects.requireNonNull(member).getIdLong(), System.currentTimeMillis());
                    testDataByUserMap.remove(member);
                    System.out.println("2Сообщение изменено");
                },
                failure -> System.err.println("2Не удалось изменить оригинальное сообщение: " + failure.getMessage()));
    }

    public static void showTestFailed(Member member, Message message, LinkedHashMap<Long, Long> lastTestAttemptByUser, LinkedHashMap<Member, TestDataByUser> testDataByUserMap, int MAX_QUESTIONS, String CLOSE_TEST_ID) {
        TestDataByUser currentTestData = testDataByUserMap.get(member);
        EmbedBuilder lossEmbed = new EmbedBuilder();
        lossEmbed.setColor(new Color(158, 26, 26));
        lossEmbed.setTitle("Тест провален :cry:");
        lossEmbed.setDescription("- Вы ответили правильно на " + (currentTestData.getCurrentQuestionNumber() - 1) + " из " + MAX_QUESTIONS + " вопросов.\n" +
                "- Перепройди тест через 10 минут.");


        message.editMessageEmbeds(lossEmbed.build()).setActionRow(Button.danger(CLOSE_TEST_ID, "Закрыть")).queue(
                success -> {
                    lastTestAttemptByUser.put(Objects.requireNonNull(member).getIdLong(), System.currentTimeMillis());
                    testDataByUserMap.remove(member);
                    System.out.println("2Сообщение изменено");
                },
                failure -> System.err.println("2Не удалось изменить оригинальное сообщение: " + failure.getMessage()));

    }



}
