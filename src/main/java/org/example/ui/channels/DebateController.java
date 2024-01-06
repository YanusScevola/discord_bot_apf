package org.example.ui.channels;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.example.audio.lavaplayer.PlayerManager;
import org.example.ui.enums.Stage;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.resources.StringRes;
import org.example.ui.constants.RolesID;
import org.example.ui.enums.Winner;
import org.example.utils.StageTimer;
import org.jetbrains.annotations.NotNull;

public class DebateController {
    private static final int GOVERNMENT_DEBATERS_LIMIT = 1;
    private static final int OPPOSITION_DEBATERS_LIMIT = 0;

    private static final int DEBATERS_PREPARATION_TIME = 15 * 60;
    private static final int HEAD_GOVERNMENT_FIRST_SPEECH_TIME = 2;
    private static final int HEAD_OPPOSITION_FIRST_SPEECH_TIME = 2;
    private static final int MEMBER_GOVERNMENT_SPEECH_TIME = 2;
    private static final int MEMBER_OPPOSITION_SPEECH_TIME = 20;
    private static final int OPPONENT_ASK_TIME = 15; // 15
    private static final int HEAD_OPPOSITION_LAST_SPEECH_TIME = 5;
    private static final int HEAD_GOVERNMENT_LAST_SPEECH_TIME = 5;
    private static final int JUDGES_PREPARATION_TIME = 5;

    private static final String END_DEBATE_BTN_ID = "end_debate";
    private static final String END_SPEECH_BTN_ID = "end_speech";
    private static final String ASK_QUESTION_BTN_ID = "ask_question";
    private static final String VOTE_GOVERNMENT_BTN_ID = "vote_government";
    private static final String VOTE_OPPOSITION_BTN_ID = "vote_opposition";

    private final Button askQuestionButton = Button.success(ASK_QUESTION_BTN_ID, "Задать вопрос");
    private final Button endSpeechButton = Button.primary(END_SPEECH_BTN_ID, "Закончить речь");
    private final Button endDebateButton = Button.danger(END_DEBATE_BTN_ID, "Закончить дебаты");
    private final Button voteGovernmentButton = Button.primary(VOTE_GOVERNMENT_BTN_ID, "Голосовать за \"Правительство\"");
    private final Button voteOppositionButton = Button.primary(VOTE_OPPOSITION_BTN_ID, "Голосовать за \"Оппозицию\"");

    private final ApiRepository apiRepository;
    private final DbRepository dbRepository;
    private final StringRes stringsRes;

    private VoiceChannel judgesVoiceChannel;
    private VoiceChannel tribuneVoiceChannel;
    private VoiceChannel governmentVoiceChannel;
    private VoiceChannel oppositionVoiceChannel;

    private Member bot;
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
    private Winner winner;


    public DebateController(ApiRepository apiRepository, DbRepository dbRepository) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        this.stringsRes = StringRes.getInstance(StringRes.Language.RUSSIAN);
    }

    public void onJoinToVoiceChannel(@NotNull GuildVoiceUpdateEvent event) {
        handelMembersMute(event);
        initDebateMembers(event);
        startDebate(event);
    }

    private void handelMembersMute(GuildVoiceUpdateEvent event) {
        Member member = event.getMember();
        if (member.equals(event.getGuild().getSelfMember())) {
            bot = member;
        } else {
            disableMicrophone(member, null);
        }

    }

    public void onLeaveFromVoiceChannel(GuildVoiceUpdateEvent event) {
        enableMicrophone(event.getMember(), () -> {
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
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.TRIBUNE_CHANNEL_NAME))) tribuneVoiceChannel = channel;
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.JUDGE_CHANNEL_NAME))) judgesVoiceChannel = channel;
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.GOVERNMENT_CHANNEL_NAME))) governmentVoiceChannel = channel;
        if (voiceChannelName.equals(stringsRes.get(StringRes.Key.OPPOSITION_CHANNEL_NAME))) oppositionVoiceChannel = channel;
    }

    private void onClickAskQuestionBtn(ButtonInteractionEvent event) {
        if (allDebaters.contains(event.getMember())) {
            if (currentStage == Stage.MEMBER_GOVERNMENT_SPEECH) {
                if (oppositionDebaters.contains(event.getMember())) {
                    apiRepository.showEphemeralMessage(event, "Приготовьтесь задать вопрос.");
                    startAskOpponent(event.getGuild(), event.getMember(), memberGovernment);
                } else {
                    apiRepository.showEphemeralMessage(event, "Нельзя задавать вопрос члену своей команды");
                }
            } else if (currentStage == Stage.MEMBER_OPPOSITION_SPEECH) {
                if (governmentDebaters.contains(event.getMember())) {
                    startAskOpponent(event.getGuild(), event.getMember(), memberOpposition);
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

    private void onClickEndSpeechBtn(ButtonInteractionEvent event) {
        apiRepository.showEphemeralMessage(event, "Речь закончена.");
        skipTimer();
    }

    private void onClickEndDebateBtn(ButtonInteractionEvent event) {
        if (allDebaters.contains(event.getMember())) {
            apiRepository.showEphemeralMessage(event, "Данная функция пока не реализована.");
        }else {
            apiRepository.showEphemeralMessage(event, "Вы не дебатер");
        }
    }

    private void onClickVoteGovernmentBtn(ButtonInteractionEvent event) {
        if (judges.contains(event.getMember())) {
            apiRepository.showEphemeralMessage(event, "Вы проголосовали за \"Правительство\"");
            winner = Winner.GOVERNMENT;
        }else {
            apiRepository.showEphemeralMessage(event, "Вы не судья");
        }
    }

    private void onClickVoteOppositionBtn(ButtonInteractionEvent event) {
        if (judges.contains(event.getMember())) {
            apiRepository.showEphemeralMessage(event, "Вы проголосовали за \"Оппозицию\"");
            winner = Winner.OPPOSITION;
        }else {
            apiRepository.showEphemeralMessage(event, "Вы не судья");
        }
    }

    private void initDebateMembers(GuildVoiceUpdateEvent event) {
        List<Role> roles = event.getMember().getRoles();
        Member member = event.getMember();

        if (currentStage != Stage.DEBATERS_PREPARATION && currentStage != Stage.JUDGES_PREPARATION) {
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
        String title = "Подготовка дебатеров.";
        String audioPath = "Подготовка дебатеров.mp3";
        String audioPath2 = "Разговоры пока дебатеры готовятся.mp3";
        String audioPathWaiting = "Фоновая музыка.mp3";
        List<Button> buttons = List.of(endDebateButton);

        playAudio(guild, audioPath, () -> {
            sendTheme("Легализация оружия");
            moveMembers(oppositionDebaters.stream().toList(), oppositionVoiceChannel, () -> {
                moveMembers(governmentDebaters.stream().toList(), governmentVoiceChannel, () -> {
                    startTimer(currentStageTimer, title, DEBATERS_PREPARATION_TIME, buttons, () -> {
                        disableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                            moveMembers(allDebaters.stream().toList(), tribuneVoiceChannel, () -> {
                                stopAudio(guild);
                                startStage(guild, Stage.HEAD_GOVERNMENT_FIRST_SPEECH);
                            });
                        });
                    });
                    playAudio(guild, audioPath2, () -> enableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                        enableMicrophone(tribuneVoiceChannel.getMembers(), null);
                        playAudio(guild, audioPathWaiting, null);
                    }));
                });
            });
        });
    }

    private void startHeadGovernmentFirstSpeechStage(Guild guild) {
        currentStage = Stage.HEAD_GOVERNMENT_FIRST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Вступ глава правительства.mp3";
        String title = "Вступительная речь главы правительства.";
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);
//        guild.getAudioManager().setSendingHandler(new KeepAliveHandler());

        playAudio(guild, audioPath, () -> {
            enableMicrophone(headGovernment, () -> {
                startTimer(currentStageTimer, title, HEAD_GOVERNMENT_FIRST_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(headGovernment, () -> {
                        startStage(guild, Stage.HEAD_OPPOSITION_FIRST_SPEECH);
                    });
                });
            });
        });
    }

    private void startHeadOppositionFirstSpeechStage(Guild guild) {
        currentStage = Stage.HEAD_OPPOSITION_FIRST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Вступ глава оппозиции.mp3";
        String title = "Вступительная речь главы оппозиции.";
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);

        playAudio(guild, audioPath, () -> {
            enableMicrophone(headOpposition, () -> {
                startTimer(currentStageTimer, title, HEAD_OPPOSITION_FIRST_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(headOpposition, () -> {
                        startStage(guild, Stage.MEMBER_GOVERNMENT_SPEECH);
                    });
                });
            });
        });
    }

    private void startMemberGovernmentSpeechStage(Guild guild) {
        currentStage = Stage.MEMBER_GOVERNMENT_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Член правительства.mp3";
        String title = "Речь члена правительства.";
        List<Button> buttons = Arrays.asList(askQuestionButton, endSpeechButton, endDebateButton);

        playAudio(guild, audioPath, () -> {
            enableMicrophone(memberGovernment, () -> {
                startTimer(currentStageTimer, title, MEMBER_GOVERNMENT_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(memberGovernment, () -> {
                        startStage(guild, Stage.MEMBER_OPPOSITION_SPEECH);
                    });
                });
            });
        });
    }

    private void startMemberOppositionSpeechStage(Guild guild) {
        currentStage = Stage.MEMBER_OPPOSITION_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Член оппозиции.mp3";
        String title = "Речь члена оппозиции.";
        List<Button> buttons = Arrays.asList(askQuestionButton, endSpeechButton, endDebateButton);

        playAudio(guild, audioPath, () -> {
            enableMicrophone(memberOpposition, () -> {
                startTimer(currentStageTimer, title, MEMBER_OPPOSITION_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(memberOpposition, () -> {
                        startStage(guild, Stage.HEAD_OPPOSITION_LAST_SPEECH);
                    });
                });
            });
        });
    }

    private void startAskOpponent(Guild guild, Member asker, Member answerer) {
        disableMicrophone(answerer, () -> {
            pauseTimer(OPPONENT_ASK_TIME, 6, () -> {
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
        String audioPath = "Закл глава оппозиции.mp3";
        String title = "Заключительная речь главы оппозиции.";
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);

        playAudio(guild, audioPath, () -> {
            enableMicrophone(headOpposition, () -> {
                startTimer(currentStageTimer, title, HEAD_OPPOSITION_LAST_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(headOpposition, () -> {
                        startStage(guild, Stage.HEAD_GOVERNMENT_LAST_SPEECH);
                    });
                });
            });
        });
    }

    private void startHeadGovernmentLastSpeechStage(Guild guild) {
        currentStage = Stage.HEAD_GOVERNMENT_LAST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Закл глава правительства.mp3";
        String title = "Заключительная речь главы правительства.";
        List<Button> buttons = Arrays.asList(endSpeechButton, endDebateButton);

        playAudio(guild, audioPath, () -> {
            enableMicrophone(headGovernment, () -> {
                startTimer(currentStageTimer, title, HEAD_GOVERNMENT_LAST_SPEECH_TIME, buttons, () -> {
                    disableMicrophone(headGovernment, () -> {
                        startStage(guild, Stage.JUDGES_PREPARATION);
                    });
                });
            });
        });
    }

    private void startJudgesPreparationStage(Guild guild) {
        currentStage = Stage.JUDGES_PREPARATION;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String audioPath = "Подготовка судей.mp3";
        String audioPath2 = "Разговоры пока судьи готовятся.mp3";
        String audioPathWaiting = "Фоновая музыка.mp3";
        String title = "Подготовка судей.";

        playAudio(guild, audioPath, () -> {
            moveMembers(judges.stream().toList(), judgesVoiceChannel, () -> {
                startTimer(currentStageTimer, title, JUDGES_PREPARATION_TIME, new ArrayList<>(), () -> {
                    enableMicrophone(judgesVoiceChannel.getMembers(), () -> {
                        stopAudio(guild);
                        moveMembers(judges.stream().toList(), tribuneVoiceChannel, () -> {

                        });
                    });
                });
                playAudio(guild, audioPath2, () -> {
                    enableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                        playAudio(guild, audioPathWaiting, null);
                    });
                });
            });
        });
    }

    private void startVerdictStage(Guild guild) {

    }

    private void playAudio(Guild guild, String path, Runnable callback) {
        System.out.println("PLAY " + path);
        String AUDIO_BASE_PATH = "src/main/resources/audio/";
        guild.getAudioManager().setSendingHandler(PlayerManager.get().getAudioSendHandler());
        PlayerManager.get().play(guild, AUDIO_BASE_PATH + path, callback);
    }

    private void stopAudio(Guild guild) {
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

    public void sendVotingMessage() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Голосование:");
        eb.setDescription("За правительство: 2 \nЗа оппозицию: 1");
        eb.setColor(0xF40C0C);

        judgesVoiceChannel.sendMessageEmbeds(eb.build())
                .setActionRow(voteGovernmentButton, voteOppositionButton).queue();
    }


}
