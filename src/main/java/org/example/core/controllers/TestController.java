//package org.example.core.controllers;
//
//import net.dv8tion.jda.api.EmbedBuilder;
//import net.dv8tion.jda.api.entities.Member;
//import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
//import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
//import net.dv8tion.jda.api.interactions.InteractionHook;
//import net.dv8tion.jda.api.interactions.components.buttons.Button;
//import org.example.core.constants.RolesID;
//import org.example.core.models.Question;
//import org.example.domain.UseCase;
//
//import java.awt.*;
//import java.util.*;
//import java.util.List;
//
//public class TestController {
////    private static final String START_TEST_ID = "start_test";
////    private static final String ANSWER_A_ID = "answer_a";
////    private static final String ANSWER_B_ID = "answer_b";
////    private static final String ANSWER_C_ID = "answer_c";
////    private static final String ANSWER_D_ID = "answer_d";
////    private static final String CLOSE_TEST_ID = "close_test";
//
//    private static final int MAX_QUESTIONS = 10;
//
//    private static TestController instance;
//    private UseCase useCase;
//    private List<Question> questions;
//    private List<String> currentRandomAnswers;
//    private Question currentQuestion;
//    private int currentQuestionNumber = 0;
//    private int flag = 0;
//    private final Map<String, Integer> answerButtonIdByAnswersIndex = Map.of(
//            ANSWER_A_ID, 0,
//            ANSWER_B_ID, 1,
//            ANSWER_C_ID, 2,
//            ANSWER_D_ID, 3
//    );
//    private final List<String> answerButtonsIds = new ArrayList<>(List.of(ANSWER_A_ID, ANSWER_B_ID, ANSWER_C_ID, ANSWER_D_ID));
//
//    TestController(UseCase useCase, TextChannel channel, Member member) {
//        this.useCase = useCase;
//    }
//
//    public void onButtonInteraction(ButtonInteractionEvent event) {
//        if (event.getComponentId().equals(START_TEST_ID)) {
//            startTest(event);
//        } else if (answerButtonsIds.contains(event.getComponentId())) {
//            if (currentQuestionNumber >= MAX_QUESTIONS) {
//                showTestSuccess(event);
//                return;
//            }
//
//            String selectedAnswer = currentRandomAnswers.get(answerButtonIdByAnswersIndex.get(event.getComponentId()));
//            boolean isSelectedAnswerCorrect = currentQuestion.getCorrectAnswer().equals(selectedAnswer);
//
//            if (isSelectedAnswerCorrect) {
//                showQuestion(event, currentQuestionNumber++);
//            } else {
//                showTestFailed(event);
//            }
//        } else if (event.getComponentId().equals(CLOSE_TEST_ID)) {
//            event.deferEdit().queue();
//            event.getHook().deleteOriginal().queue(
//                    success -> System.out.println("Эфемерное сообщение удалено"),
//                    failure -> System.err.println("Ошибка при удалении эфемерного сообщения: " + failure.getMessage())
//            );
//        }
//    }
//
//    public void startTest(ButtonInteractionEvent event) {
//        showQuestion(event, 0);
//
//    }
////
////    public void showNeedToStartTestEmbed(InteractionHook message) {
////
////        EmbedBuilder embed = new EmbedBuilder();
////        embed.setColor(new Color(88, 100, 242));
////        embed.setTitle("У вас нету роли дебатер.\nЧтобы получить роль нужно пройти тест на знание правил.");
////        embed.setDescription("""
////                - Чтобы подгоовиться к тесту прочитайте правила дебатов АПФ.\s
////                - В тесте 20 вопросов, и 4 варианта ответа.\s
////                - На каждый вопрос выделяется 30 секунд.
////                - Тест автоматически закончится через 15 минут.
////                """);
////
////        Button startTestButton = Button.success(START_TEST_ID, "Начать тест");
////
////        message.editOriginalEmbeds(embed.build())
////                .setActionRow(startTestButton)
////                .queue(
////                        success -> System.out.println("1Сообщение изменено"),
////                        failure -> System.err.println("1Не удалось изменить оригинальное сообщение: ")
////                );
////    }
//
////    public void showQuestion(ButtonInteractionEvent event, int questionIndex) {
////        System.out.println(flag);
////        flag++;
////        useCase.getAllQuestions().thenAccept(questions -> {
////            this.questions = new ArrayList<>(questions);
////            Collections.shuffle(this.questions);
////
////            if (questions.isEmpty()) {
////                System.err.println("Список вопросов пуст");
////                return;
////            }
////
////            Question question = questions.get(questionIndex);
////            currentQuestion = question;
////            currentQuestionNumber = questionIndex + 1;
////
////            EmbedBuilder embed = new EmbedBuilder();
////            embed.setColor(new Color(88, 100, 242));
////            embed.setTitle(question.getText());
////            embed.setFooter((currentQuestionNumber) + " из " + MAX_QUESTIONS);
////
////            currentRandomAnswers = new ArrayList<>(question.getAnswers());
////            Collections.shuffle(currentRandomAnswers);
////
////            String answersText = "**A**: " + currentRandomAnswers.get(0) + "\n" +
////                    "**B**: " + currentRandomAnswers.get(1) + "\n" +
////                    "**C**: " + currentRandomAnswers.get(2) + "\n" +
////                    "**D**: " + currentRandomAnswers.get(3);
////            embed.setDescription(answersText);
////
////            Button a = Button.primary(ANSWER_A_ID, "A");
////            Button b = Button.primary(ANSWER_B_ID, "B");
////            Button c = Button.primary(ANSWER_C_ID, "C");
////            Button d = Button.primary(ANSWER_D_ID, "D");
////
////            event.editMessageEmbeds(embed.build())
////                    .setActionRow(a, b, c, d)
////                    .queue(
////                            success -> System.out.println("1Сообщение изменено"),
////                            failure -> System.err.println("1Не удалось изменить оригинальное сообщение: " + failure.getMessage())
////                    );
////        });
////    }
////
////    public void showTestSuccess(ButtonInteractionEvent event) {
////        Map<Member, Long> members = Map.of(Objects.requireNonNull(event.getMember()), RolesID.DEBATER_APF);
////        useCase.addRolesToMembers(members).thenAccept(success -> {
////            EmbedBuilder winEmbed = new EmbedBuilder();
////            winEmbed.setColor(new Color(36, 128, 70));
////            winEmbed.setTitle("Тест пройден");
////            winEmbed.setDescription("Вы правельно ответили на " + MAX_QUESTIONS + " вопросов.");
////
////            event.editMessageEmbeds(winEmbed.build()).queue(
////                    success1 -> System.out.println("Сообщение изменено"),
////                    failure -> System.err.println("Не удалось изменить оригинальное сообщение: " + failure.getMessage()));
////        });
////    }
////
////    public void showTestFailed(ButtonInteractionEvent event) {
////        EmbedBuilder lossEmbed = new EmbedBuilder();
////        lossEmbed.setColor(new Color(255, 0, 0));
////        lossEmbed.setTitle("Тест провален :cry:");
////        lossEmbed.setDescription("- Вы правельно ответили на " + (currentQuestionNumber) + " из " + MAX_QUESTIONS + " вопросов.\n" +
////                "- Попробуйте еще раз через 10 минут.");
////
////        event.editMessageEmbeds(lossEmbed.build()).setActionRow(Button.danger(CLOSE_TEST_ID, "Закрыть")).queue(
////                success -> System.out.println("2Сообщение изменено"),
////                failure -> System.err.println("2Не удалось изменить оригинальное сообщение: " + failure.getMessage()));
////    }
//
//}
