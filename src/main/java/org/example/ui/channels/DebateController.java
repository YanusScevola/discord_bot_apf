package org.example.ui.channels;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.example.lavaplayer.PlayerManager;
import org.example.lavaplayer.TrackScheduler;
import org.example.ui.enums.Stage;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.resources.StringRes;
import org.example.ui.constants.RolesID;
import org.example.ui.interfaces.MuteSuccessCallback;
import org.example.utils.StageTimer;
import org.example.utils.Utils;
import org.jetbrains.annotations.NotNull;

public class DebateController {
    private static final int GOVERNMENT_DEBATERS_LIMIT = 1;
    private static final int OPPOSITION_DEBATERS_LIMIT = 0;

    private static final int DEBATERS_PREPARATION_TIME = 5;
    private static final int HEAD_GOVERNMENT_FIRST_SPEECH_TIME = 2;
    private static final int HEAD_OPPOSITION_FIRST_SPEECH_TIME = 2;
    private static final int MEMBER_GOVERNMENT_SPEECH_TIME = 2;
    private static final int MEMBER_OPPOSITION_SPEECH_TIME = 20;
    private static final int OPPONENT_ASK_TIME = 15; // 15
    private static final int HEAD_OPPOSITION_LAST_SPEECH_TIME = 5;
    private static final int HEAD_GOVERNMENT_LAST_SPEECH_TIME = 5;
    private static final int JUDGES_PREPARATION_TIME = 5;

    private static final String BTN_END_DEBATE_ID = "end_debate";
    private static final String BTN_END_SPEECH_ID = "end_speech";
    private static final String BTN_ASK_QUESTION_ID = "ask_question";

    private final Button askQuestionButton = Button.success(BTN_ASK_QUESTION_ID, "Задать вопрос");
    private final Button endSpeechButton = Button.primary(BTN_END_SPEECH_ID, "Закончить речь");
    private final Button endDebateButton = Button.danger(BTN_END_DEBATE_ID, "Закончить дебаты");

    private final ApiRepository apiRepository;
    private final DbRepository dbRepository;
    private final StringRes stringsRes;

    private VoiceChannel judgesVoiceChannel;
    private VoiceChannel tribuneVoiceChannel;
    private VoiceChannel governmentVoiceChannel;
    private VoiceChannel oppositionVoiceChannel;

    private Member headGovernment;
    private Member headOpposition;
    private Member memberGovernment;
    private Member memberOpposition;

    private final Set<Member> allDebaters = new HashSet<>();
    private final Set<Member> governmentDebaters = new HashSet<>();
    private final Set<Member> oppositionDebaters = new HashSet<>();
    private final Set<Member> judges = new HashSet<>();

    private Stage currentStage = Stage.START_DEBATE;
    private StageTimer currentStageTimer;
    private boolean isDebateStarted = false;

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
            onClickEndDebateBtn(event);
        } else if (Objects.equals(event.getButton().getId(), BTN_END_SPEECH_ID)) {
            onClickEndSpeechBtn(event);
        } else if (Objects.equals(event.getButton().getId(), BTN_ASK_QUESTION_ID)) {
            onClickAskQuestionBtn(event);
        }
    }

    public void addChannel(VoiceChannel channel) {
        String voiceChannelName = channel.getName();
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.TRIBUNE_CHANNEL_NAME))) tribuneVoiceChannel = channel;
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.JUDGE_CHANNEL_NAME))) judgesVoiceChannel = channel;
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.GOVERNMENT_CHANNEL_NAME))) governmentVoiceChannel = channel;
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.OPPOSITION_CHANNEL_NAME))) oppositionVoiceChannel = channel;
    }


    private void onClickEndDebateBtn(ButtonInteractionEvent event) {

    }

    private void onClickEndSpeechBtn(ButtonInteractionEvent event) {
        apiRepository.showEphemeralMessage(event, "Вы досрочно закончили речь.");
        if (allDebaters.contains(event.getMember())) {
            if (currentStage == Stage.HEAD_GOVERNMENT_FIRST_SPEECH) {
                startStage(event.getGuild(), Stage.HEAD_OPPOSITION_FIRST_SPEECH);
            } else if (currentStage == Stage.HEAD_OPPOSITION_FIRST_SPEECH) {
                startStage(event.getGuild(), Stage.MEMBER_GOVERNMENT_SPEECH);
            } else if (currentStage == Stage.MEMBER_GOVERNMENT_SPEECH) {
                startStage(event.getGuild(), Stage.MEMBER_OPPOSITION_SPEECH);
            } else if (currentStage == Stage.MEMBER_OPPOSITION_SPEECH) {
                startStage(event.getGuild(), Stage.HEAD_OPPOSITION_LAST_SPEECH);
            } else if (currentStage == Stage.HEAD_OPPOSITION_LAST_SPEECH) {
                startStage(event.getGuild(), Stage.HEAD_GOVERNMENT_LAST_SPEECH);
            } else if (currentStage == Stage.HEAD_GOVERNMENT_LAST_SPEECH) {
                startStage(event.getGuild(), Stage.JUDGES_PREPARATION);
            } else {
                apiRepository.showEphemeralMessage(event, "Сейчас не время заканчивать речь");
            }
        } else {
            apiRepository.showEphemeralMessage(event, "Вы не дебатер");
        }

    }

    private void onClickAskQuestionBtn(ButtonInteractionEvent event) {
        apiRepository.showEphemeralMessage(event, "Приготовьтесь задать вопрос.");
        if (allDebaters.contains(event.getMember())) {
            if (currentStage == Stage.MEMBER_GOVERNMENT_SPEECH) {
                if (oppositionDebaters.contains(event.getMember())) {
                    startStage(event.getGuild(), Stage.OPPONENT_ASK);
                } else {
                    apiRepository.showEphemeralMessage(event, "Нельзя задавать вопрос члену своей команды");
                }
            } else if (currentStage == Stage.MEMBER_OPPOSITION_SPEECH) {
                if (governmentDebaters.contains(event.getMember())) {
                    startStage(event.getGuild(), Stage.OPPONENT_ASK);
                } else {
                    apiRepository.showEphemeralMessage(event, "Нельзя задавать вопрос члену своей команды");
                }
            } else {
                apiRepository.showEphemeralMessage(event, "Сейчас не время задавать вопрос");
            }
        } else {
            apiRepository.showEphemeralMessage(event, "Вы не дебатер");
        }
    }

    private void initDebateMembers(GuildVoiceUpdateEvent event) {
        List<Role> roles = event.getMember().getRoles();
        Member member = event.getMember();
        if (roles.contains(event.getGuild().getRoleById(RolesID.HEAD_GOVERNMENT))) {
            headGovernment = member;
            governmentDebaters.add(member);
        }
        if (roles.contains(event.getGuild().getRoleById(RolesID.HEAD_OPPOSITION))) {
            headOpposition = member;
            oppositionDebaters.add(member);
        }
        if (roles.contains(event.getGuild().getRoleById(RolesID.MEMBER_GOVERNMENT))) {
            memberGovernment = member;
            governmentDebaters.add(member);
        }
        if (roles.contains(event.getGuild().getRoleById(RolesID.MEMBER_OPPOSITION))) {
            memberOpposition = member;
            oppositionDebaters.add(member);
        }
        if (roles.contains(event.getGuild().getRoleById(RolesID.JUDGE))) {
            judges.add(member);
        }
    }

    private void startDebate(GuildVoiceUpdateEvent event) {
        boolean isGovernmentDebatersReady = governmentDebaters.size() == GOVERNMENT_DEBATERS_LIMIT;
        boolean isOppositionDebatersReady = oppositionDebaters.size() == OPPOSITION_DEBATERS_LIMIT;
        boolean isJudgesReady = judges.size() >= 1;

        if (isGovernmentDebatersReady && isOppositionDebatersReady && isJudgesReady && !isDebateStarted) {
            isDebateStarted = true;
            allDebaters.addAll(governmentDebaters);
            allDebaters.addAll(oppositionDebaters);
            startStage(event.getMember().getGuild(), Stage.START_DEBATE);
        }
    }

    private void startStage(Guild guild, @NotNull Stage stage) {
        switch (stage) {
            case START_DEBATE -> startGreetingsStage(guild);
            case DEBATERS_PREPARATION -> startDebatersPreparationStage(guild);
            case HEAD_GOVERNMENT_FIRST_SPEECH -> startHeadGovernmentFirstSpeechStage(guild);
            case HEAD_OPPOSITION_FIRST_SPEECH -> startHeadOppositionFirstSpeechStage(guild);
            case MEMBER_GOVERNMENT_SPEECH -> startMemberGovernmentSpeechStage(guild);
            case MEMBER_OPPOSITION_SPEECH -> startMemberOppositionSpeechStage(guild);
            case OPPONENT_ASK -> startOpponentAskStage(guild);
            case HEAD_OPPOSITION_LAST_SPEECH -> startHeadOppositionLastSpeechStage(guild);
            case HEAD_GOVERNMENT_LAST_SPEECH -> startHeadGovernmentLastSpeechStage(guild);
            case JUDGES_PREPARATION -> startJudgesPreparationStage(guild);
        }
        currentStage = stage;
    }

    private void startGreetingsStage(Guild guild) {
        isDebateStarted = true;
        play(guild, "Приветствие.mp3", (track) -> startStage(guild, Stage.DEBATERS_PREPARATION));
    }

    private void startDebatersPreparationStage(Guild guild) {
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = "Подготовка дебатеров.";
        String audioPath = "Подготовка дебатеров.mp3";
        String audioPath2 = "Разговоры пока дебатеры готовятся.mp3";
        List<Button> buttons = List.of(endDebateButton);

        play(guild, audioPath, (track) -> {
            sendTheme("Легализация оружия");
            Utils.startTimer(3, () -> {
                CompletableFuture<Void> oppositionMove = apiRepository.moveMembersAsync(oppositionDebaters, oppositionVoiceChannel);
                CompletableFuture<Void> governmentMove = apiRepository.moveMembersAsync(governmentDebaters, governmentVoiceChannel);
                oppositionMove.thenCompose(v -> governmentMove).thenRun(() -> {
                    play(guild, audioPath2, null);
                    start(currentStageTimer, title, DEBATERS_PREPARATION_TIME, buttons, (t) -> {
                        apiRepository.moveMembersAsync(allDebaters, tribuneVoiceChannel).thenRun(() -> {
                            startStage(guild, Stage.HEAD_GOVERNMENT_FIRST_SPEECH);
                        });
                    });
                });
            });
        });
    }

    private void startHeadGovernmentFirstSpeechStage(Guild guild) {
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Вступ глава правительства.mp3";
        String title = "Вступительная речь главы правительства.";
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);

        play(guild, audioPath, (track) -> {
            start(currentStageTimer, title, HEAD_GOVERNMENT_FIRST_SPEECH_TIME, buttons, (t) -> {
                startStage(guild, Stage.HEAD_OPPOSITION_FIRST_SPEECH);
            });
        });
    }

    private void startHeadOppositionFirstSpeechStage(Guild guild) {
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Вступ глава оппозиции.mp3";
        String title = "Вступительная речь главы оппозиции.";
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);

        play(guild, audioPath, (track) -> {
            start(currentStageTimer, title, HEAD_OPPOSITION_FIRST_SPEECH_TIME, buttons, (t) -> {
                startStage(guild, Stage.MEMBER_GOVERNMENT_SPEECH);
            });
        });
    }

    private void startMemberGovernmentSpeechStage(Guild guild) {
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Член правительства.mp3";
        String title = "Речь члена правительства.";
        List<Button> buttons = Arrays.asList(askQuestionButton, endSpeechButton, endDebateButton);

        play(guild, audioPath, (track) -> {
            start(currentStageTimer, title, MEMBER_GOVERNMENT_SPEECH_TIME, buttons, (t) -> {
                startStage(guild, Stage.MEMBER_OPPOSITION_SPEECH);
            });
        });
    }

    private void startMemberOppositionSpeechStage(Guild guild) {
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Член оппозиции.mp3";
        String title = "Речь члена оппозиции.";
        List<Button> buttons = Arrays.asList(askQuestionButton, endSpeechButton, endDebateButton);

        play(guild, audioPath, (track) -> {
            start(currentStageTimer, title, MEMBER_OPPOSITION_SPEECH_TIME, buttons, (t) -> {
                startStage(guild, Stage.HEAD_OPPOSITION_LAST_SPEECH);
            });
        });
    }

    private void startOpponentAskStage(Guild guild) {
        pauseByTime(OPPONENT_ASK_TIME, 5);
        play(guild, "Опонент задает вопрос.mp3", null);
    }

    private void startHeadOppositionLastSpeechStage(Guild guild) {
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Закл глава оппозиции.mp3";
        String title = "Заключительная речь главы оппозиции.";
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);

        play(guild, audioPath, (track) -> {
            start(currentStageTimer, title, HEAD_OPPOSITION_LAST_SPEECH_TIME, buttons, (t) -> {
                startStage(guild, Stage.HEAD_GOVERNMENT_LAST_SPEECH);
            });
        });
    }

    private void startHeadGovernmentLastSpeechStage(Guild guild) {
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Закл глава правительства.mp3";
        String title = "Заключительная речь главы правительства.";
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);

        play(guild, audioPath, (track) -> {
            start(currentStageTimer, title, HEAD_GOVERNMENT_LAST_SPEECH_TIME, buttons, (t) -> {
                startStage(guild, Stage.JUDGES_PREPARATION);
            });
        });
    }

    private void startJudgesPreparationStage(Guild guild) {
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Подготовка судей.mp3";
        String audioPath2 = "Разговоры пока судьи готовятся.mp3";
        String title = "Подготовка судей.";

        play(guild, audioPath, (track) -> {
            apiRepository.moveMembersAsync(judges, judgesVoiceChannel).thenRun(() -> {
                play(guild, audioPath2, null);
                start(currentStageTimer, title, JUDGES_PREPARATION_TIME, new ArrayList<>(), (t) -> {
                    apiRepository.moveMembersAsync(judges, tribuneVoiceChannel).thenRun(() -> {
                    });
                });
            });
        });
    }

    private void play(Guild guild, String path, TrackScheduler.TrackEndCallback callback) {
        System.out.println("PLAY");
        String AUDIO_BASE_PATH = "src/main/resources/audio/";
        PlayerManager.get().play(guild, AUDIO_BASE_PATH + path, callback);
    }

    private void start(StageTimer timer, String title, long time, List<Button> buttons,
                       Consumer<String> timerCallback) {
        System.out.println("START");
        timer.start(title, time, buttons, timerCallback);
    }

    private void pauseByTime(long time, long delay) {
        System.out.println("PAUSE");
        currentStageTimer.pause(time, delay);
    }

    public void sendTheme(String theme) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Тема: " + theme).setColor(Color.GREEN);
        tribuneVoiceChannel.sendMessageEmbeds(embedBuilder.build()).queue();
    }

    public void muteMember(Member member, MuteSuccessCallback callback) {
        if (member == null) {
            System.out.println("Предоставленный участник недействителен");
            return;
        }

        AuditableRestAction<Void> action = member.mute(true);

        action.queue(
                (success) -> {
                    System.out.println("Микрофон пользователя успешно отключен");
                    callback.onSuccess();
                },
                (error) -> System.out.println("Произошла ошибка при попытке отключить микрофон: " + error.getMessage())
        );
    }
}
