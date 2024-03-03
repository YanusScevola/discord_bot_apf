package org.example.core.controllers;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.*;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.utils.FileUpload;
import org.example.core.enums.Stage;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.player.PlayerManager;
import org.example.recorder.MyAudioReceiveHandler;
import org.example.resources.StringRes;
import org.example.core.constants.RolesID;
import org.example.core.enums.Winner;
import org.example.utils.StageTimer;
import org.jetbrains.annotations.NotNull;

public class DebateController {
    private static final int GOVERNMENT_DEBATERS_LIMIT = 1;
    private static final int OPPOSITION_DEBATERS_LIMIT = 0;

    private static final int DEBATERS_PREPARATION_TIME = 15; // 15
    private static final int HEAD_GOVERNMENT_FIRST_SPEECH_TIME = 7; // 7
    private static final int HEAD_OPPOSITION_FIRST_SPEECH_TIME = 8; // 8
    private static final int MEMBER_GOVERNMENT_SPEECH_TIME = 8; // 8
    private static final int MEMBER_OPPOSITION_SPEECH_TIME = 8; // 8
    private static final int OPPONENT_ASK_TIME = 15; // 15
    private static final int HEAD_OPPOSITION_LAST_SPEECH_TIME = 4; // 4
    private static final int HEAD_GOVERNMENT_LAST_SPEECH_TIME = 5; // 5
    private static final int JUDGES_PREPARATION_TIME = 15; // 15

    private static final String END_DEBATE_BTN_ID = "end_debate";
    private static final String END_SPEECH_BTN_ID = "end_speech";
    private static final String ASK_QUESTION_BTN_ID = "ask_question";
    private static final String VOTE_GOVERNMENT_BTN_ID = "vote_government";
    private static final String VOTE_OPPOSITION_BTN_ID = "vote_opposition";

    private final Button askQuestionButton;
    private final Button endSpeechButton;
    private final Button endDebateButton;
    private final Button voteGovernmentButton;
    private final Button voteOppositionButton;

    private final ApiRepository apiRepository;
    private final DbRepository dbRepository;
    private final StringRes stringsRes;

    private SubscribeController subscribeController;

    private VoiceChannel judgesVoiceChannel;
    private VoiceChannel tribuneVoiceChannel;
    private VoiceChannel governmentVoiceChannel;
    private VoiceChannel oppositionVoiceChannel;

    private Member bot;
    private Member headGovernment;
    private Member headOpposition;
    private Member memberGovernment;
    private Member memberOpposition;

    private final Set<VoiceChannel> allVoiceChannels = new HashSet<>();

    private final Set<Member> allDebaters = new HashSet<>();
    private final Set<Member> governmentDebaters = new HashSet<>();
    private final Set<Member> oppositionDebaters = new HashSet<>();
    private final Set<Member> judges = new HashSet<>();
    private final Set<Member> votedJudges = new HashSet<>();

    private Stage currentStage = Stage.START_DEBATE;
    private StageTimer currentStageTimer;
    private boolean isDebateStarted = false;
    private int votesForGovernment = 0;
    private int votesForOpposition = 0;
    private Message votingMessage;
    private Winner winner = Winner.GOVERNMENT;
    MyAudioReceiveHandler audioHandler = new MyAudioReceiveHandler();


    public DebateController(ApiRepository apiRepository, DbRepository dbRepository, StringRes stringsRes, SubscribeController subscribeController) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        this.stringsRes = stringsRes;
        this.subscribeController = subscribeController;

        askQuestionButton = Button.success(ASK_QUESTION_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_ASK_QUESTION));
        endSpeechButton = Button.primary(END_SPEECH_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_END_SPEECH));
        endDebateButton = Button.danger(END_DEBATE_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_END_DEBATE));
        voteGovernmentButton = Button.primary(VOTE_GOVERNMENT_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_VOTE_GOVERNMENT));
        voteOppositionButton = Button.primary(VOTE_OPPOSITION_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_VOTE_OPPOSITION));
    }

    public void onJoinToVoiceChannel(@NotNull GuildVoiceUpdateEvent event) {
        handelMembersMute(event);
        initDebateMembers(event);
        startDebate(event);
    }

    public void onLeaveFromVoiceChannel(GuildVoiceUpdateEvent event) {
        boolean isChannelEmpty = tribuneVoiceChannel != null && tribuneVoiceChannel.getMembers().isEmpty();
        enableMicrophone(event.getMember(), () -> {
            if (isDebateStarted && isChannelEmpty) {
                deleteVoiceChannels(allVoiceChannels.stream().toList(), () -> {
                    subscribeController.restartDebate();
                });
            }
        });
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        switch (Objects.requireNonNull(event.getButton().getId())) {
            case END_DEBATE_BTN_ID -> onClickEndDebateBtn(event);
            case END_SPEECH_BTN_ID -> onClickEndSpeechBtn(event);
            case ASK_QUESTION_BTN_ID -> onClickAskQuestionBtn(event);
            case VOTE_GOVERNMENT_BTN_ID -> onClickVoteGovernmentBtn(event);
            case VOTE_OPPOSITION_BTN_ID -> onClickVoteOppositionBtn(event);
        }
    }

    public void addChannel(VoiceChannel channel) {
        String voiceChannelName = channel.getName();
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE)))
            tribuneVoiceChannel = channel;
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.CHANNEL_JUDGE)))
            judgesVoiceChannel = channel;
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.CHANNEL_GOVERNMENT)))
            governmentVoiceChannel = channel;
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.CHANNEL_OPPOSITION)))
            oppositionVoiceChannel = channel;
        allVoiceChannels.add(channel);
    }

    private void onClickAskQuestionBtn(ButtonInteractionEvent event) {
        apiRepository.showEphemeralLoading(event, (message) -> {
            if (allDebaters.contains(event.getMember())) {
                if (currentStage == Stage.MEMBER_GOVERNMENT_SPEECH) {
                    if (oppositionDebaters.contains(event.getMember())) {
                        message.editOriginal(stringsRes.get(StringRes.Key.REMARK_ASK_GOVERNMENT_MEMBER)).queue((msg) -> {
                            startAskOpponent(event.getGuild(), event.getMember(), memberGovernment);
                        });
                    } else
                        message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NOT_ASK_OWN_TEAM)).queue();
                } else if (currentStage == Stage.MEMBER_OPPOSITION_SPEECH) {
                    if (governmentDebaters.contains(event.getMember())) {
                        message.editOriginal(stringsRes.get(StringRes.Key.REMARK_ASK_OPPOSITION_MEMBER)).queue((msg) -> {
                            startAskOpponent(event.getGuild(), event.getMember(), memberGovernment);
                        });
                    } else
                        message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NOT_ASK_OWN_TEAM)).queue();
                } else message.editOriginal("Сейчас нельзя задавать вопросы").queue();
            } else message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NOT_DEBATER)).queue();
        });
    }

    private void onClickEndSpeechBtn(ButtonInteractionEvent event) {
        apiRepository.showEphemeralLoading(event, (message) -> {
            Member member = event.getMember();
            boolean canEndSpeech = (currentStage == Stage.HEAD_GOVERNMENT_FIRST_SPEECH && headGovernment.equals(member)) ||
                    (currentStage == Stage.HEAD_OPPOSITION_FIRST_SPEECH && headOpposition.equals(member)) ||
                    (currentStage == Stage.MEMBER_GOVERNMENT_SPEECH && memberGovernment.equals(member)) ||
                    (currentStage == Stage.MEMBER_OPPOSITION_SPEECH && memberOpposition.equals(member)) ||
                    (currentStage == Stage.HEAD_GOVERNMENT_LAST_SPEECH && headGovernment.equals(member)) ||
                    (currentStage == Stage.HEAD_OPPOSITION_LAST_SPEECH && headOpposition.equals(member));

            if (canEndSpeech) {
                message.editOriginal(stringsRes.get(StringRes.Key.REMARK_SPEECH_END)).queue((msg) -> {
                    skipTimer();
                });
            } else {
                message.editOriginal("Вы не можете закончить чужую речь").queue();
            }
        });
    }

    private void onClickEndDebateBtn(ButtonInteractionEvent event) {
        apiRepository.showEphemeralLoading(event, (message) -> {
            if (allDebaters.contains(event.getMember())) {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NOT_IMPLEMENTED)).queue();
            } else {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NOT_DEBATER)).queue();
            }
        });
    }

    private void onClickVoteGovernmentBtn(ButtonInteractionEvent event) {
        apiRepository.showEphemeralLoading(event, (message) -> {
            if (judges.contains(event.getMember())) {
                voteForWinner(event, Winner.GOVERNMENT, () -> {
                    message.editOriginal(stringsRes.get(StringRes.Key.REMARK_VOTE_GOVERNMENT)).queue();
                });
            } else {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NOT_JUDGE)).queue();
            }
        });
    }

    private void onClickVoteOppositionBtn(ButtonInteractionEvent event) {
        apiRepository.showEphemeralLoading(event, (message) -> {
            if (judges.contains(event.getMember())) {
                voteForWinner(event, Winner.OPPOSITION, () -> {
                    message.editOriginal(stringsRes.get(StringRes.Key.REMARK_VOTE_OPPOSITION)).queue();
                });
            } else {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NOT_JUDGE)).queue();
            }
        });
    }

    private void handelMembersMute(GuildVoiceUpdateEvent event) {
        Member member = event.getMember();
        if (member.equals(event.getGuild().getSelfMember())) {
            bot = member;
        } else {
            disableMicrophone(member, null);
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
//            startStage(event.getMember().getGuild(), Stage.START_DEBATE);

            try {
                AudioManager audioManager = event.getGuild().getAudioManager();
                MyAudioReceiveHandler audioReceiveHandler = new MyAudioReceiveHandler();
                audioManager.setReceivingHandler(audioReceiveHandler);
                audioManager.openAudioConnection(tribuneVoiceChannel);
//                audioReceiveHandler.startRecording();
                System.out.println("Запись начата");
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            System.out.println("Запись завершена");
//                            audioReceiveHandler.stopRecording("/Users/vasagrigoras/IdeaProjects/DiscordBotAPF/audio.wav");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, 10000);
            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }
//
//    public File getRecordingFile() {
//        String filePath = "audio.pcm";
//
//        File recordingFile = new File(filePath);
//        if (recordingFile.exists()) {
//            return recordingFile;
//        } else {
//            System.out.println("Файл записи не найден.");
//            return null;
//        }
//    }
//
//
    public void sendRecording(Guild guild, File file, String channelId) {
        if (file != null) {
            TextChannel channel = guild.getTextChannelById(channelId);
            if (channel != null) {
                FileUpload f = FileUpload.fromData(file, file.getName());
                channel.sendFiles(f).queue();
            } else {
                System.out.println("Текстовый канал не найден.");
            }
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
            case HEAD_OPPOSITION_LAST_SPEECH -> startHeadOppositionLastSpeechStage(guild);
            case HEAD_GOVERNMENT_LAST_SPEECH -> startHeadGovernmentLastSpeechStage(guild);
            case JUDGES_PREPARATION -> startJudgesPreparationStage(guild);
            case JUDGES_VERDICT -> startVerdictStage(guild);
        }
    }

    private void startGreetingsStage(Guild guild) {
        isDebateStarted = true;
        currentStage = Stage.START_DEBATE;
        playAudio(guild, "Приветствие.mp3", () -> startStage(guild, Stage.DEBATERS_PREPARATION));
    }

    private void startDebatersPreparationStage(Guild guild) {
        currentStage = Stage.DEBATERS_PREPARATION;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_DEBATER_PREPARATION);
        List<Button> buttons = List.of(endDebateButton);

        playAudio(guild, "Подготовка дебатеров.mp3", () -> {
            sendTheme("ЭП хочет отменить традиционное образование в пользу индивидуализированных образовательных траекторий.");
            moveMembers(oppositionDebaters.stream().toList(), oppositionVoiceChannel, () -> {
                moveMembers(governmentDebaters.stream().toList(), governmentVoiceChannel, () -> {
                    startTimer(currentStageTimer, title, DEBATERS_PREPARATION_TIME, buttons, () -> {
                        disableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                            moveMembers(allDebaters.stream().toList(), tribuneVoiceChannel, () -> {
                                stopCurrentAudio(guild);
                                startStage(guild, Stage.HEAD_GOVERNMENT_FIRST_SPEECH);
                            });
                        });
                    });
                    playAudio(guild, "Разговоры пока дебатеры готовятся.mp3", () -> enableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                        enableMicrophone(tribuneVoiceChannel.getMembers(), null);
                        playAudio(guild, "Фоновая музыка.mp3", null);
                    }));
                });
            });
        });
    }

    private void startHeadGovernmentFirstSpeechStage(Guild guild) {
        currentStage = Stage.HEAD_GOVERNMENT_FIRST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_HEAD_GOVERNMENT_FIRST_SPEECH);
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);

        playAudio(guild, "Вступ глава правительства.mp3", () -> {
            enableMicrophone(headGovernment, () -> {
                playAudio(guild, "Тишина.mp3", null);
                startTimer(currentStageTimer, title, HEAD_GOVERNMENT_FIRST_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(headGovernment, () -> {
                        stopCurrentAudio(guild);
                        startStage(guild, Stage.HEAD_OPPOSITION_FIRST_SPEECH);
                    });
                });
            });
        });
    }

    private void startHeadOppositionFirstSpeechStage(Guild guild) {
        currentStage = Stage.HEAD_OPPOSITION_FIRST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_HEAD_OPPOSITION_FIRST_SPEECH);
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);

        playAudio(guild, "Вступ глава оппозиции.mp3", () -> {
            enableMicrophone(headOpposition, () -> {
                playAudio(guild, "Тишина.mp3", null);
                startTimer(currentStageTimer, title, HEAD_OPPOSITION_FIRST_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(headOpposition, () -> {
                        stopCurrentAudio(guild);
                        startStage(guild, Stage.MEMBER_GOVERNMENT_SPEECH);
                    });
                });
            });
        });
    }

    private void startMemberGovernmentSpeechStage(Guild guild) {
        currentStage = Stage.MEMBER_GOVERNMENT_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_MEMBER_GOVERNMENT_SPEECH);
        List<Button> buttons = Arrays.asList(askQuestionButton, endSpeechButton, endDebateButton);

        playAudio(guild, "Член правительства.mp3", () -> {
            enableMicrophone(memberGovernment, () -> {
                playAudio(guild, "Тишина.mp3", null);
                startTimer(currentStageTimer, title, MEMBER_GOVERNMENT_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(memberGovernment, () -> {
                        stopCurrentAudio(guild);
                        startStage(guild, Stage.MEMBER_OPPOSITION_SPEECH);
                    });
                });
            });
        });
    }

    private void startMemberOppositionSpeechStage(Guild guild) {
        currentStage = Stage.MEMBER_OPPOSITION_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_MEMBER_OPPOSITION_SPEECH);
        List<Button> buttons = Arrays.asList(askQuestionButton, endSpeechButton, endDebateButton);

        playAudio(guild, "Член оппозиции.mp3", () -> {
            enableMicrophone(memberOpposition, () -> {
                playAudio(guild, "Тишина.mp3", null);
                startTimer(currentStageTimer, title, MEMBER_OPPOSITION_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(memberOpposition, () -> {
                        stopCurrentAudio(guild);
                        startStage(guild, Stage.HEAD_OPPOSITION_LAST_SPEECH);
                    });
                });
            });
        });
    }

    private void startAskOpponent(Guild guild, Member asker, Member answerer) {
        stopCurrentAudio(guild);
        disableMicrophone(answerer, () -> {
            pauseTimer(OPPONENT_ASK_TIME, 6, () -> {
                playAudio(guild, "Тишина.mp3", null);
                disableMicrophone(asker, () -> {
                    enableMicrophone(answerer, this::resumeTimer);
                });
            });
            playAudio(guild, "Опонент задает вопрос.mp3", () -> {
                enableMicrophone(asker, () -> {

                });
            });
        });
    }

    private void startHeadOppositionLastSpeechStage(Guild guild) {
        currentStage = Stage.HEAD_OPPOSITION_LAST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_HEAD_OPPOSITION_LAST_SPEECH);
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);

        playAudio(guild, "Закл глава оппозиции.mp3", () -> {
            enableMicrophone(headOpposition, () -> {
                playAudio(guild, "Тишина.mp3", null);
                startTimer(currentStageTimer, title, HEAD_OPPOSITION_LAST_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(headOpposition, () -> {
                        stopCurrentAudio(guild);
                        startStage(guild, Stage.HEAD_GOVERNMENT_LAST_SPEECH);
                    });
                });
            });
        });
    }

    private void startHeadGovernmentLastSpeechStage(Guild guild) {
        currentStage = Stage.HEAD_GOVERNMENT_LAST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_HEAD_GOVERNMENT_LAST_SPEECH);
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);

        playAudio(guild, "Закл глава правительства.mp3", () -> {
            enableMicrophone(headGovernment, () -> {
                playAudio(guild, "Тишина.mp3", null);
                startTimer(currentStageTimer, title, HEAD_GOVERNMENT_LAST_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(headGovernment, () -> {
                        stopCurrentAudio(guild);
                        startStage(guild, Stage.JUDGES_PREPARATION);
                    });
                });
            });
        });
    }

    private void startJudgesPreparationStage(Guild guild) {
        currentStage = Stage.JUDGES_PREPARATION;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_JUDGES_PREPARATION);

        playAudio(guild, "Подготовка судей.mp3", () -> {
            moveMembers(judges.stream().toList(), judgesVoiceChannel, () -> {
                sendVotingMessage();
                startTimer(currentStageTimer, title, JUDGES_PREPARATION_TIME, new ArrayList<>(), () -> {
                    disableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                        stopCurrentAudio(guild);
                        moveMembers(judges.stream().toList(), tribuneVoiceChannel, () -> {
                            startStage(guild, Stage.JUDGES_VERDICT);
                        });
                    });
                });
                playAudio(guild, "Разговоры пока судьи готовятся.mp3", () -> {
                    playAudio(guild, "Фоновая музыка.mp3", () -> {
                        enableMicrophone(tribuneVoiceChannel.getMembers(), null);
                    });
                });
            });
        });
    }

    private void startVerdictStage(Guild guild) {
        currentStage = Stage.JUDGES_VERDICT;
        switch (winner) {
            case GOVERNMENT -> {
                playAudio(guild, "Победа правительства.mp3", () -> {
                    enableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                        AudioManager audioManager = guild.getAudioManager();
                        if (audioManager.isConnected()) {
                            audioManager.closeAudioConnection();
                            System.out.println("Бот покинул голосовой канал.");
                        } else {
                            System.out.println("Бот уже не находится в голосовом канале.");
                        }
                    });
                });
            }
            case OPPOSITION -> {
                playAudio(guild, "Победа оппозиции.mp3", () -> {
                    enableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                        AudioManager audioManager = guild.getAudioManager();
                        if (audioManager.isConnected()) {
                            audioManager.closeAudioConnection();
                            System.out.println("Бот покинул голосовой канал.");
                        } else {
                            System.out.println("Бот уже не находится в голосовом канале.");
                        }
                    });
                });
            }
        }
    }

    private void playAudio(Guild guild, String path, Runnable callback) {
        System.out.println("PLAY " + path);
        String AUDIO_BASE_PATH = "src/main/resources/audio/";
        guild.getAudioManager().setSendingHandler(PlayerManager.get().getAudioSendHandler());
        PlayerManager.get().play(guild, AUDIO_BASE_PATH + path, callback);
    }

    private void stopCurrentAudio(Guild guild) {
        System.out.println("STOP " + PlayerManager.get().getGuildMusicManager(guild).getPlayer().getPlayingTrack().getInfo().title);
        PlayerManager.get().stop(guild);
    }

    private void startTimer(StageTimer timer, String title, long time, List<Button> buttons, Runnable timerCallback) {
        System.out.println("START " + title);
        timer.start(title, time, buttons, timerCallback);
    }

    private void skipTimer() {
        System.out.println("SKIP");
        currentStageTimer.skip();
    }

    private void pauseTimer(long time, long delay, Runnable timerCallback) {
        System.out.println("PAUSE");
        currentStageTimer.pause(time, delay, timerCallback);
    }

    private void resumeTimer() {
        System.out.println("RESUME");
        currentStageTimer.resume();
    }

    private void sendTheme(String theme) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Тема: " + theme).setColor(Color.GREEN);
        tribuneVoiceChannel.sendMessageEmbeds(embedBuilder.build()).queue();
    }

    private void moveMembers(List<Member> members, VoiceChannel targetChannel, Runnable callback) {
        apiRepository.moveMembers(members, targetChannel, callback);
    }

    private void enableMicrophone(List<Member> members, Runnable callback) {
        List<Member> modifiableMembers = new ArrayList<>(members);
        modifiableMembers.remove(bot);
        apiRepository.muteMembers(modifiableMembers, false, callback);
    }

    private void enableMicrophone(Member member, Runnable callback) {
        if (member != null) {
            apiRepository.muteMembers(List.of(member), false, callback);
        } else {
            callback.run();
        }
    }

    private void disableMicrophone(List<Member> members, Runnable callback) {
        List<Member> modifiableMembers = new ArrayList<>(members);
        modifiableMembers.remove(bot);
        apiRepository.muteMembers(modifiableMembers, true, callback);
    }

    private void disableMicrophone(Member member, Runnable callback) {
        if (member != null) {
            apiRepository.muteMembers(List.of(member), true, callback);
        } else {
            callback.run();
        }
    }

    private void deleteVoiceChannels(List<VoiceChannel> channels, Runnable callback) {
        apiRepository.deleteVoiceChannels(channels, callback);
    }

    public void sendVotingMessage() {
        System.out.println("SEND VOTING MESSAGE");
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(stringsRes.get(StringRes.Key.TITLE_VOTING));
        eb.setDescription(stringsRes.get(StringRes.Key.DESCRIPTION_VOTE_FOR_GOVERNMENT) + votesForGovernment + stringsRes.get(StringRes.Key.DESCRIPTION_VOTE_FOR_OPPOSITION) + votesForOpposition);
        eb.setColor(0xF40C0C);

        judgesVoiceChannel.sendMessageEmbeds(eb.build())
                .setActionRow(voteGovernmentButton, voteOppositionButton)
                .queue(message -> votingMessage = message);
    }

    private void voteForWinner(ButtonInteractionEvent event, Winner voteFor, Runnable callback) {
        System.out.println("VOTE FOR " + voteFor);
        if (votedJudges.contains(event.getMember())) {
            apiRepository.showEphemeralLoading(event, (message) -> {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_ALREADY_VOTED)).queue();
            });
            return;
        }
        votedJudges.add(event.getMember());
        if (voteFor == Winner.GOVERNMENT) {
            votesForGovernment++;
        } else if (voteFor == Winner.OPPOSITION) {
            votesForOpposition++;
        }
        updateVotingMessage();
        if (callback != null) callback.run();

        if (votedJudges.size() == judges.size()) {
            if (votesForGovernment > votesForOpposition) {
                winner = Winner.GOVERNMENT;
            } else if (votesForGovernment < votesForOpposition) {
                winner = Winner.OPPOSITION;
            }
            System.out.println("WINNER " + winner);
            disableVotingButtons();
            stopCurrentAudio(event.getGuild());
            skipTimer();
        }
    }

    private void updateVotingMessage() {
        System.out.println("UPDATE VOTING MESSAGE");
        if (votingMessage == null) {
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Голосование:");
        eb.setDescription("За правительство: " + votesForGovernment + "\nЗа оппозицию: " + votesForOpposition);
        eb.setColor(0xF40C0C);

        System.out.println("UPDATE VOTING MESSAGE2");
        votingMessage.editMessageEmbeds(eb.build()).queue();
    }

    private void disableVotingButtons() {
        System.out.println("DISABLE VOTING BUTTONS");
        if (votingMessage == null) {
            return;
        }
        votingMessage.editMessageComponents(
                ActionRow.of(voteGovernmentButton.asDisabled(), voteOppositionButton.asDisabled())
        ).queue();
        System.out.println("DISABLE VOTING BUTTONS2");
    }


}
