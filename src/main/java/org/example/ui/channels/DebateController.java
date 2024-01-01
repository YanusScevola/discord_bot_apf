package org.example.ui.channels;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import org.example.lavaplayer.PlayerManager;
import org.example.ui.enums.Stage;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.resources.StringRes;
import org.example.ui.constants.RolesID;
import org.example.utils.StageTimer;
import org.example.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class DebateController {
    private static final int GOVERNMENT_DEBATERS_LIMIT = 1;
    private static final int OPPOSITION_DEBATERS_LIMIT = 0;

    private static final int DEBATERS_PREPARATION_TIME = 5;
    private static final int HEAD_GOVERNMENT_FIRST_SPEECH_TIME = 5;
    private static final int HEAD_OPPOSITION_FIRST_SPEECH_TIME = 5;
    private static final int MEMBER_GOVERNMENT_SPEECH_TIME = 5;
    private static final int OPPONENT_ASK_MEMBER_GOVERNMENT = 5;
    private static final int OPPONENT_ASK_MEMBER_OPPOSITION = 5;
    private static final int MEMBER_OPPOSITION_SPEECH_TIME = 5;
    private static final int HEAD_OPPOSITION_LAST_SPEECH_TIME = 5;
    private static final int HEAD_GOVERNMENT_LAST_SPEECH_TIME = 5;
    private static final int JUDGES_PREPARATION_TIME = 5;

    private static final String BTN_END_DEBATE_ID = "end_debate";
    private static final String BTN_END_SPEECH_ID = "end_speech";
    private static final String BTN_ASK_QUESTION_ID = "ask_question";

    private final Button endDebateButton = Button.danger(BTN_END_DEBATE_ID, "Закончить дебаты");
    private final Button endSpeechButton = Button.success(BTN_END_SPEECH_ID, "Закончить речь");
    private final Button askQuestionButton = Button.primary(BTN_ASK_QUESTION_ID, "Задать вопрос");

    private final ApiRepository apiRepository;
    private final DbRepository dbRepository;
    private final StringRes stringsRes;

    private VoiceChannel judgesVoiceChannel;
    private VoiceChannel tribuneVoiceChannel;
    private VoiceChannel governmentVoiceChannel;
    private VoiceChannel oppositionVoiceChannel;
    private StageTimer stageTimer;

    private final String AUDIO_BASE_PATH = "src/main/resources/audio/";
    private final PlayerManager player = PlayerManager.get();
    private boolean isDebateStarted = false;

    private final Set<Member> allDebaters = new HashSet<>();
    private final Set<Member> governmentDebaters = new HashSet<>();
    private final Set<Member> oppositionDebaters = new HashSet<>();
    private final Set<Member> judges = new HashSet<>();

    private Stage currentStage = Stage.START_DEBATE;


    public DebateController(ApiRepository apiRepository, DbRepository dbRepository) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        this.stringsRes = StringRes.getInstance(StringRes.Language.RUSSIAN);
    }

    public void onJoinToVoiceChannel(@NotNull GuildVoiceUpdateEvent event) {
        initDebateMembers(event);
        startDebate(event);
    }

    public void onLeaveFromVoiceChannel(GuildVoiceUpdateEvent event) {
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (Objects.equals(event.getButton().getId(), BTN_END_DEBATE_ID)) {
            onEndDebateButton(event);
        } else if (Objects.equals(event.getButton().getId(), BTN_END_SPEECH_ID)) {
            onEndSpeechButton(event);
        } else if (Objects.equals(event.getButton().getId(), BTN_ASK_QUESTION_ID)) {
            onAskQuestionButton(event);
        }
    }

    private void onEndDebateButton(ButtonInteractionEvent event) {

    }

    private void onEndSpeechButton(ButtonInteractionEvent event) {

    }

    private void onAskQuestionButton(ButtonInteractionEvent event) {
        if (allDebaters.contains(event.getMember())) {
            switch (currentStage) {
                case MEMBER_GOVERNMENT_SPEECH -> startStage(event.getGuild(), Stage.OPPONENT_ASK_MEMBER_GOVERNMENT);
                case MEMBER_OPPOSITION_SPEECH -> startStage(event.getGuild(), Stage.OPPONENT_ASK_MEMBER_OPPOSITION);
            }
        } else {
            apiRepository.showEphemeralMessage(event, "Вы не являетесь дебатером");
        }
    }

    public void addChannel(VoiceChannel channel) {
        if (channel.getName().equals(stringsRes.get(StringRes.Key.TRIBUNE_CHANNEL_NAME))) tribuneVoiceChannel = channel;
        if (channel.getName().equals(stringsRes.get(StringRes.Key.JUDGE_CHANNEL_NAME))) judgesVoiceChannel = channel;
        if (channel.getName().equals(stringsRes.get(StringRes.Key.GOVERNMENT_CHANNEL_NAME)))
            governmentVoiceChannel = channel;
        if (channel.getName().equals(stringsRes.get(StringRes.Key.OPPOSITION_CHANNEL_NAME)))
            oppositionVoiceChannel = channel;

        this.stageTimer = new StageTimer(tribuneVoiceChannel);
    }

    private void initDebateMembers(GuildVoiceUpdateEvent event) {
        List<String> govDebaterIds = List.of(RolesID.HEAD_GOVERNMENT, RolesID.HEAD_OPPOSITION);
        List<String> oppDebaterIds = List.of(RolesID.MEMBER_GOVERNMENT, RolesID.MEMBER_OPPOSITION);
        List<String> roleJudgeIds = List.of(RolesID.JUDGE);

        event.getMember().getRoles().forEach(role -> {
            if (govDebaterIds.contains(role.getId())) governmentDebaters.add(event.getMember());
            if (oppDebaterIds.contains(role.getId())) oppositionDebaters.add(event.getMember());
            if (roleJudgeIds.contains(role.getId())) judges.add(event.getMember());
        });

        allDebaters.addAll(governmentDebaters);
        allDebaters.addAll(oppositionDebaters);
    }

    private void startDebate(GuildVoiceUpdateEvent event) {
        boolean isGovernmentDebatersReady = governmentDebaters.size() == GOVERNMENT_DEBATERS_LIMIT;
        boolean isOppositionDebatersReady = oppositionDebaters.size() == OPPOSITION_DEBATERS_LIMIT;
        boolean isJudgesReady = judges.size() >= 1;

        if (isGovernmentDebatersReady && isOppositionDebatersReady && isJudgesReady && !isDebateStarted) {
            isDebateStarted = true;
            startStage(event.getMember().getGuild(), Stage.START_DEBATE);
        }
    }

    private void startStage(Guild guild, Stage stage) {
        currentStage = stage;
        switch (stage) {
            case START_DEBATE -> startGreetingsStage(guild);
            case DEBATERS_PREPARATION -> startDebatersPreparationStage(guild);
            case HEAD_GOVERNMENT_FIRST_SPEECH -> startHeadGovernmentFirstSpeechStage(guild);
            case HEAD_OPPOSITION_FIRST_SPEECH -> startHeadOppositionFirstSpeechStage(guild);
            case MEMBER_GOVERNMENT_SPEECH -> startMemberGovernmentSpeechStage(guild);
            case OPPONENT_ASK_MEMBER_GOVERNMENT -> startOpponentAskGovernmentStage(guild);
            case MEMBER_OPPOSITION_SPEECH -> startMemberOppositionSpeechStage(guild);
            case OPPONENT_ASK_MEMBER_OPPOSITION -> startOpponentAskOppositionStage(guild);
            case HEAD_OPPOSITION_LAST_SPEECH -> startHeadOppositionLastSpeechStage(guild);
            case HEAD_GOVERNMENT_LAST_SPEECH -> startHeadGovernmentLastSpeechStage(guild);
            case JUDGES_PREPARATION -> startJudgesPreparationStage(guild);
            case PAUSE -> startPause(guild);
            case RESUME -> startResume(guild);
        }
    }


    private void startGreetingsStage(Guild guild) {
        isDebateStarted = true;
        player.play(guild, AUDIO_BASE_PATH + "Приветствие.mp3", (track) -> {
            startStage(guild, Stage.DEBATERS_PREPARATION);
        });
    }

    private void startDebatersPreparationStage(Guild guild) {
        List<Button> buttons = List.of(endDebateButton);

        player.play(guild, AUDIO_BASE_PATH + "Подготовка дебатеров.mp3", (track) -> {
            sendTheme("Легализация оружия");
            Utils.startTimer(2, () -> {
                CompletableFuture<Void> oppositionMove = apiRepository.moveMembersAsync(oppositionDebaters, oppositionVoiceChannel);
                CompletableFuture<Void> governmentMove = apiRepository.moveMembersAsync(governmentDebaters, governmentVoiceChannel);
                oppositionMove.thenCompose(v -> governmentMove).thenRun(() -> {
                    player.play(guild, AUDIO_BASE_PATH + "Разговоры пока дебатеры готовятся.mp3", null);
                    stageTimer.start("Дебатеры подготавливаются.", DEBATERS_PREPARATION_TIME, buttons, (title) -> {
                        apiRepository.moveMembersAsync(allDebaters, tribuneVoiceChannel).thenRun(() -> {
                            startStage(guild, Stage.HEAD_GOVERNMENT_FIRST_SPEECH);
                        });
                    });
                });
            });
        });
    }

    private void startHeadGovernmentFirstSpeechStage(Guild guild) {
        List<Button> buttons = Arrays.asList(endDebateButton, endSpeechButton);
        player.play(guild, AUDIO_BASE_PATH + "Вступ глава правительства.mp3", (track) -> {
            stageTimer.start("Вступительная речь главы правительства.", HEAD_GOVERNMENT_FIRST_SPEECH_TIME, buttons, (title) -> {
                startStage(guild, Stage.HEAD_OPPOSITION_FIRST_SPEECH);
            });
        });
    }

    private void startHeadOppositionFirstSpeechStage(Guild guild) {
        List<Button> buttons = Arrays.asList(endDebateButton, endSpeechButton);
        player.play(guild, AUDIO_BASE_PATH + "Вступ глава оппозиции.mp3", (track) -> {
            stageTimer.start("Вступительная речь главы оппозиции.", HEAD_OPPOSITION_FIRST_SPEECH_TIME, buttons, (title) -> {
                startStage(guild, Stage.MEMBER_GOVERNMENT_SPEECH);
            });
        });
    }

    private void startMemberGovernmentSpeechStage(Guild guild) {
        List<Button> buttons = Arrays.asList(endDebateButton, endSpeechButton, askQuestionButton);
        player.play(guild, AUDIO_BASE_PATH + "Член правительства.mp3", (track) -> {
            stageTimer.start("Речь члена правительства.", MEMBER_GOVERNMENT_SPEECH_TIME, buttons, (title) -> {
                startStage(guild, Stage.MEMBER_OPPOSITION_SPEECH);
            });
        });
    }

    private void startOpponentAskGovernmentStage(Guild guild) {
        player.play(guild, AUDIO_BASE_PATH + "Опонент задает вопрос", (track) -> {
            stageTimer.start("Вопрос к члену Правительства", OPPONENT_ASK_MEMBER_GOVERNMENT, new ArrayList<>(), (title) -> {
                startStage(guild, Stage.RESUME);
            });
        });
    }

    private void startMemberOppositionSpeechStage(Guild guild) {
        List<Button> buttons = Arrays.asList(endDebateButton, endSpeechButton, askQuestionButton);
        player.play(guild, AUDIO_BASE_PATH + "Член оппозиции.mp3", (track) -> {
            stageTimer.start("Речь члена оппозиции.", MEMBER_OPPOSITION_SPEECH_TIME, buttons, (title) -> {
                startStage(guild, Stage.HEAD_OPPOSITION_LAST_SPEECH);
            });
        });
    }

    private void startOpponentAskOppositionStage(Guild guild) {
        stageTimer.pause();
        player.play(guild, AUDIO_BASE_PATH + "Опонент задает вопрос", (track) -> {
            stageTimer.start("Вопрос к члену оппозиции", OPPONENT_ASK_MEMBER_OPPOSITION, new ArrayList<>(), (title) -> {
                startStage(guild, Stage.RESUME);
            });
        });
    }


    private void startHeadOppositionLastSpeechStage(Guild guild) {
        List<Button> buttons = Arrays.asList(endDebateButton, endSpeechButton);
        player.play(guild, AUDIO_BASE_PATH + "Закл глава оппозиции.mp3", (track) -> {
            stageTimer.start("Заключительная речь главы оппозиции.", HEAD_OPPOSITION_LAST_SPEECH_TIME, buttons, (title) -> {
                startStage(guild, Stage.HEAD_GOVERNMENT_LAST_SPEECH);
            });
        });
    }

    private void startHeadGovernmentLastSpeechStage(Guild guild) {
        List<Button> buttons = Arrays.asList(endDebateButton, endSpeechButton);
        player.play(guild, AUDIO_BASE_PATH + "Закл глава правительства.mp3", (track) -> {
            stageTimer.start("Заключительная речь главы правительства.", HEAD_GOVERNMENT_LAST_SPEECH_TIME, buttons, (title) -> {
                startStage(guild, Stage.JUDGES_PREPARATION);
            });
        });
    }

    private void startJudgesPreparationStage(Guild guild) {
        player.play(guild, AUDIO_BASE_PATH + "Подготовка судей.mp3", (track) -> {
            apiRepository.moveMembersAsync(judges, judgesVoiceChannel).thenRun(() -> {
                player.play(guild, AUDIO_BASE_PATH + "Разговоры пока судьи готовятся.mp3", null);
                stageTimer.start("Судьи подготавливаются.", JUDGES_PREPARATION_TIME, new ArrayList<>(), (title) -> {
                    apiRepository.moveMembersAsync(judges, tribuneVoiceChannel).thenRun(() -> {

                    });
                });
            });
        });
    }


    private void startPause(Guild guild) {

    }

    private void startResume(Guild guild) {

    }

    public void sendTheme(String theme) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Тема: " + theme).setColor(Color.GREEN);
        tribuneVoiceChannel.sendMessageEmbeds(embedBuilder.build()).queue();
    }


}