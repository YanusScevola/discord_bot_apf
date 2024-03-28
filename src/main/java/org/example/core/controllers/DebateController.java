package org.example.core.controllers;

import java.awt.Color;
import java.time.LocalDateTime;
import java.util.*;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.managers.AudioManager;
import org.example.core.constants.enums.Stage;
import org.example.core.models.Debate;
import org.example.domain.UseCase;
import org.example.core.player.PlayerManager;
import org.example.resources.StringRes;
import org.example.core.constants.RolesID;
import org.example.core.constants.enums.Winner;
import org.example.core.utils.StageTimer;
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
    private static final int WAITING_MEMBER_IN_TRIBUNE_TIME = 10; // 60

    private static final String END_SPEECH_BTN_ID = "end_speech";
    private static final String ASK_QUESTION_BTN_ID = "ask_question";
    private static final String VOTE_GOVERNMENT_BTN_ID = "vote_government";
    private static final String VOTE_OPPOSITION_BTN_ID = "vote_opposition";

    private final Button askQuestionButton;
    private final Button endSpeechButton;
    private final Button voteGovernmentButton;
    private final Button voteOppositionButton;

    private final UseCase useCase;
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
    private boolean isWaitingMemberInTribuneForSpeak = false;
    private int votesForGovernment = 0;
    private int votesForOpposition = 0;
    private Message votingMessage;
    private Message waitingMessage;
    private Winner winner = Winner.NO_WINNER;
    private Timer waitingMemberInTribuneTimer;


    public DebateController(UseCase useCase, StringRes stringsRes, SubscribeController subscribeController) {
        this.useCase = useCase;
        this.stringsRes = stringsRes;
        this.subscribeController = subscribeController;

        askQuestionButton = Button.success(ASK_QUESTION_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_ASK_QUESTION));
        endSpeechButton = Button.primary(END_SPEECH_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_END_SPEECH));
        voteGovernmentButton = Button.primary(VOTE_GOVERNMENT_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_VOTE_GOVERNMENT));
        voteOppositionButton = Button.primary(VOTE_OPPOSITION_BTN_ID, stringsRes.get(StringRes.Key.BUTTON_VOTE_OPPOSITION));
    }

    public void onJoinToTribuneVoiceChannel(Guild guild, AudioChannel channelJoined, Member member) {
        initDebateMember(guild, member);

        boolean isGovernmentDebatersReady = governmentDebaters.size() == GOVERNMENT_DEBATERS_LIMIT;
        boolean isOppositionDebatersReady = oppositionDebaters.size() == OPPOSITION_DEBATERS_LIMIT;
        boolean isJudgesReady = judges.size() >= 1;
        boolean isBotJoined = member.equals(bot);

        disableMicrophone(List.of(member), null);

        if (isGovernmentDebatersReady && isOppositionDebatersReady && isJudgesReady && !isDebateStarted) {
            startDebate(guild);
        }

        if (isDebateStarted) {
            handelWaitingMemberInTribuneForSpeak(guild, channelJoined, member);
        }

        if (currentStage == Stage.JUDGES_PREPARATION) {
            if (new HashSet<>(judgesVoiceChannel.getMembers()).containsAll(judges)) {
                startStage(guild, Stage.JUDGES_VERDICT);
            }
        }
    }

    public void onLeaveFromTribuneVoiceChannel(Guild guild, AudioChannel channelLeft, Member member) {
        useCase.enabledMicrophone(List.of(member));
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        switch (Objects.requireNonNull(event.getButton().getId())) {
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
        useCase.showEphemeralLoading(event).thenAccept(message -> {
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
        useCase.showEphemeralLoading(event).thenAccept(message -> {
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
        useCase.showEphemeralLoading(event).thenAccept(message -> {
            if (allDebaters.contains(event.getMember())) {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NOT_IMPLEMENTED)).queue();
            } else {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NOT_DEBATER)).queue();
            }
        });
    }

    private void onClickVoteGovernmentBtn(ButtonInteractionEvent event) {
        useCase.showEphemeralLoading(event).thenAccept(message -> {
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
        useCase.showEphemeralLoading(event).thenAccept(message -> {
            if (judges.contains(event.getMember())) {
                voteForWinner(event, Winner.OPPOSITION, () -> {
                    message.editOriginal(stringsRes.get(StringRes.Key.REMARK_VOTE_OPPOSITION)).queue();
                });
            } else {
                message.editOriginal(stringsRes.get(StringRes.Key.WARNING_NOT_JUDGE)).queue();
            }
        });
    }

    private void initDebateMember(Guild guild, Member member) {
        List<Role> roles = member.getRoles();

        if (roles.contains(guild.getRoleById(RolesID.HEAD_GOVERNMENT))) {
            headGovernment = member;
            governmentDebaters.add(member);
        }
        if (roles.contains(guild.getRoleById(RolesID.HEAD_OPPOSITION))) {
            headOpposition = member;
            oppositionDebaters.add(member);
        }
        if (roles.contains(guild.getRoleById(RolesID.MEMBER_GOVERNMENT))) {
            memberGovernment = member;
            governmentDebaters.add(member);
        }
        if (roles.contains(guild.getRoleById(RolesID.MEMBER_OPPOSITION))) {
            memberOpposition = member;
            oppositionDebaters.add(member);
        }
        if (roles.contains(guild.getRoleById(RolesID.JUDGE))) {
            judges.add(member);
        }
        if (member.equals(guild.getSelfMember())) {
            bot = member;
        }
    }

    private void handelWaitingMemberInTribuneForSpeak(Guild guild, AudioChannel channelJoined, Member member) {
        if (isWaitingMemberInTribuneForSpeak) {
            if (currentStage == Stage.HEAD_GOVERNMENT_FIRST_SPEECH) {
                if ((member.getRoles().contains(guild.getRoleById(RolesID.HEAD_GOVERNMENT)))) {
                    waitingMemberInTribuneTimer.cancel();
                    editMessageWithEmbed(waitingMessage, "Глава правительства вступил в трибуну");
                    startStage(channelJoined.getGuild(), Stage.HEAD_GOVERNMENT_FIRST_SPEECH);
                }
            } else if (currentStage == Stage.HEAD_OPPOSITION_FIRST_SPEECH) {
                if (member.getRoles().contains(guild.getRoleById(RolesID.HEAD_OPPOSITION))) {
                    waitingMemberInTribuneTimer.cancel();
                    editMessageWithEmbed(waitingMessage, "Глава оппозиции вступил в трибуну");
                    startStage(channelJoined.getGuild(), Stage.HEAD_OPPOSITION_FIRST_SPEECH);
                }
            } else if (currentStage == Stage.MEMBER_GOVERNMENT_SPEECH) {
                if ((member.getRoles().contains(guild.getRoleById(RolesID.MEMBER_GOVERNMENT)))) {
                    waitingMemberInTribuneTimer.cancel();
                    editMessageWithEmbed(waitingMessage, "Член правительства вступил в трибуну");
                    startStage(channelJoined.getGuild(), Stage.MEMBER_GOVERNMENT_SPEECH);
                }
            } else if (currentStage == Stage.MEMBER_OPPOSITION_SPEECH) {
                if ((member.getRoles().contains(guild.getRoleById(RolesID.MEMBER_OPPOSITION)))) {
                    waitingMemberInTribuneTimer.cancel();
                    editMessageWithEmbed(waitingMessage, "Член оппозиции вступил в трибуну");
                    startStage(channelJoined.getGuild(), Stage.MEMBER_OPPOSITION_SPEECH);
                }
            }
        }
    }

    private void startDebate(Guild guild) {
        isDebateStarted = true;
        startStage(guild, Stage.JUDGES_PREPARATION);
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
        List<Button> buttons = List.of();

        playAudio(guild, "Подготовка дебатеров.mp3", () -> {
            sendDebateTheme("ЭП хочет отменить традиционное образование в пользу индивидуализированных образовательных траекторий.");
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
        boolean isDebaterAreInTribune = tribuneVoiceChannel.getMembers().contains(headGovernment);
        currentStage = Stage.HEAD_GOVERNMENT_FIRST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_HEAD_GOVERNMENT_FIRST_SPEECH);
        List<Button> buttons = List.of(endSpeechButton);

        if (isDebaterAreInTribune) {
            playAudio(guild, "Вступ глава правительства.mp3", () -> {
                enableMicrophone(List.of(headGovernment), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, HEAD_GOVERNMENT_FIRST_SPEECH_TIME, buttons, () -> {
                        disableMicrophone(List.of(headGovernment), () -> {
                            stopCurrentAudio(guild);
                            startStage(guild, Stage.HEAD_OPPOSITION_FIRST_SPEECH);
                        });
                    });
                });
            });
        } else {
            isWaitingMemberInTribuneForSpeak = true;
            playAudio(guild, "Участник отсутствует.mp3", () -> {
                waitForMemberInTribune(guild, headGovernment, WAITING_MEMBER_IN_TRIBUNE_TIME, "Глава правительства");
            });
        }
    }

    private void startHeadOppositionFirstSpeechStage(Guild guild) {
        boolean isDebaterAreInTribune = tribuneVoiceChannel.getMembers().contains(headOpposition);
        currentStage = Stage.HEAD_OPPOSITION_FIRST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_HEAD_OPPOSITION_FIRST_SPEECH);
        List<Button> buttons = List.of(endSpeechButton);

        if (isDebaterAreInTribune) {
            playAudio(guild, "Вступ глава оппозиции.mp3", () -> {
                enableMicrophone(List.of(headOpposition), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, HEAD_OPPOSITION_FIRST_SPEECH_TIME, buttons, () -> {
                        disableMicrophone(List.of(headOpposition), () -> {
                            stopCurrentAudio(guild);
                            startStage(guild, Stage.MEMBER_GOVERNMENT_SPEECH);
                        });
                    });
                });
            });
        } else {
            isWaitingMemberInTribuneForSpeak = true;
            playAudio(guild, "Участник отсутствует.mp3", () -> {
                waitForMemberInTribune(guild, headOpposition, WAITING_MEMBER_IN_TRIBUNE_TIME, "Глава оппозиции");
            });
        }
    }

    private void startMemberGovernmentSpeechStage(Guild guild) {
        boolean isDebaterAreInTribune = tribuneVoiceChannel.getMembers().contains(memberGovernment);
        currentStage = Stage.MEMBER_GOVERNMENT_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_MEMBER_GOVERNMENT_SPEECH);
        List<Button> buttons = List.of(askQuestionButton, endSpeechButton);
        if (isDebaterAreInTribune) {
            playAudio(guild, "Член правительства.mp3", () -> {
                enableMicrophone(List.of(memberGovernment), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, MEMBER_GOVERNMENT_SPEECH_TIME, buttons, () -> {
                        disableMicrophone(List.of(memberGovernment), () -> {
                            stopCurrentAudio(guild);
                            startStage(guild, Stage.MEMBER_OPPOSITION_SPEECH);
                        });
                    });
                });
            });
        } else {
            isWaitingMemberInTribuneForSpeak = true;
            playAudio(guild, "Участник отсутствует.mp3", () -> {
                waitForMemberInTribune(guild, memberGovernment, WAITING_MEMBER_IN_TRIBUNE_TIME, "Член правительства");
            });
        }
    }

    private void startMemberOppositionSpeechStage(Guild guild) {
        boolean isDebaterAreInTribune = tribuneVoiceChannel.getMembers().contains(memberOpposition);
        currentStage = Stage.MEMBER_OPPOSITION_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_MEMBER_OPPOSITION_SPEECH);
        List<Button> buttons = List.of(askQuestionButton, endSpeechButton);
        if (isDebaterAreInTribune) {
            playAudio(guild, "Член оппозиции.mp3", () -> {
                enableMicrophone(List.of(memberOpposition), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, MEMBER_OPPOSITION_SPEECH_TIME, buttons, () -> {
                        disableMicrophone(List.of(memberOpposition), () -> {
                            stopCurrentAudio(guild);
                            startStage(guild, Stage.HEAD_OPPOSITION_LAST_SPEECH);
                        });
                    });
                });
            });
        } else {
            isWaitingMemberInTribuneForSpeak = true;
            playAudio(guild, "Участник отсутствует.mp3", () -> {
                waitForMemberInTribune(guild, memberOpposition, WAITING_MEMBER_IN_TRIBUNE_TIME, "Член оппозиции");
            });
        }
    }

    private void startHeadOppositionLastSpeechStage(Guild guild) {
        boolean isDebaterAreInTribune = tribuneVoiceChannel.getMembers().contains(headOpposition);
        currentStage = Stage.HEAD_OPPOSITION_LAST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_HEAD_OPPOSITION_LAST_SPEECH);
        List<Button> buttons = List.of(endSpeechButton);
        if (isDebaterAreInTribune) {
            playAudio(guild, "Закл глава оппозиции.mp3", () -> {
                enableMicrophone(List.of(headOpposition), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, HEAD_OPPOSITION_LAST_SPEECH_TIME, buttons, () -> {
                        disableMicrophone(List.of(headOpposition), () -> {
                            stopCurrentAudio(guild);
                            startStage(guild, Stage.HEAD_GOVERNMENT_LAST_SPEECH);
                        });
                    });
                });
            });
        } else {
            isWaitingMemberInTribuneForSpeak = true;
            playAudio(guild, "Участник отсутствует.mp3", () -> {
                waitForMemberInTribune(guild, headOpposition, WAITING_MEMBER_IN_TRIBUNE_TIME, "Глава оппозиции");
            });
        }
    }

    private void startHeadGovernmentLastSpeechStage(Guild guild) {
        boolean isDebaterAreInTribune = tribuneVoiceChannel.getMembers().contains(headGovernment);
        currentStage = Stage.HEAD_GOVERNMENT_LAST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_HEAD_GOVERNMENT_LAST_SPEECH);
        List<Button> buttons = List.of(endSpeechButton);
        if (isDebaterAreInTribune) {
            playAudio(guild, "Закл глава правительства.mp3", () -> {
                enableMicrophone(List.of(headGovernment), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, HEAD_GOVERNMENT_LAST_SPEECH_TIME, buttons, () -> {
                        disableMicrophone(List.of(headGovernment), () -> {
                            stopCurrentAudio(guild);
                            startStage(guild, Stage.JUDGES_PREPARATION);
                        });
                    });
                });
            });
        } else {
            isWaitingMemberInTribuneForSpeak = true;
            playAudio(guild, "Участник отсутствует.mp3", () -> {
                waitForMemberInTribune(guild, headGovernment, WAITING_MEMBER_IN_TRIBUNE_TIME, "Глава правительства");
            });
        }
    }

    private void startJudgesPreparationStage(Guild guild) {
        currentStage = Stage.JUDGES_PREPARATION;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = stringsRes.get(StringRes.Key.TITLE_JUDGES_PREPARATION);
        playAudio(guild, "Подготовка судей.mp3", () -> {
            playAudio(guild, "Разговоры пока судьи готовятся.mp3", () -> {
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
                            endDebate();
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
                            endDebate();
                        } else {
                            System.out.println("Бот уже не находится в голосовом канале.");
                        }
                    });
                });
            }
            case NO_WINNER -> {
                playAudio(guild, "Ничья.mp3", () -> {
                    enableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                        AudioManager audioManager = guild.getAudioManager();
                        if (audioManager.isConnected()) {
                            audioManager.closeAudioConnection();
                            endDebate();
                        } else {
                            System.out.println("Бот уже не находится в голосовом канале.");
                        }
                    });
                });
            }
        }
    }

    private void sendDebateTheme(String theme) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Тема: " + theme).setColor(Color.GREEN);
        tribuneVoiceChannel.sendMessageEmbeds(embedBuilder.build()).queue();
    }

    private void startAskOpponent(Guild guild, Member asker, Member answerer) {
        stopCurrentAudio(guild);
        disableMicrophone(List.of(answerer), () -> {
            pauseTimer(OPPONENT_ASK_TIME, 6, () -> {
                playAudio(guild, "Тишина.mp3", null);
                disableMicrophone(List.of(asker), () -> {
                    enableMicrophone(List.of(answerer), this::resumeTimer);
                });
            });
            playAudio(guild, "Опонент задает вопрос.mp3", () -> {
                enableMicrophone(List.of(asker), () -> {

                });
            });
        });
    }

    private void waitForMemberInTribune(Guild guild, Member member, long delayInSeconds, String title) {
        long currentTimeMillis = System.currentTimeMillis();
        String timerText = "Таймер: <t:" + (currentTimeMillis / 1000 + delayInSeconds) + ":R>";

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title + " отсутствует в трибуне, предлагаю подождать его подождать.");
        embed.setDescription(timerText);
        embed.setColor(Color.YELLOW);

        tribuneVoiceChannel.sendMessageEmbeds(embed.build()).queue(message -> {
            waitingMessage = message;
        });

        waitingMemberInTribuneTimer = new Timer();
        waitingMemberInTribuneTimer.schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        isWaitingMemberInTribuneForSpeak = false;
                        if (tribuneVoiceChannel.getMembers().contains(member)) {
                            startStage(guild, currentStage);
                        } else {
                            endDebate();
                        }
                    }
                },
                delayInSeconds * 1000
        );
    }

    public void editMessageWithEmbed(Message message, String newText) {
        if (message == null || newText == null) {
            return;
        }
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setDescription(newText);
        embedBuilder.setColor(Color.YELLOW);
        message.editMessageEmbeds(embedBuilder.build()).queue();
    }

    public void sendVotingMessage() {
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
            useCase.showEphemeralLoading(event).thenAccept(message -> {
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
            } else {
                endDebate();
            }
            System.out.println("WINNER " + winner);
            disableVotingButtons();
            stopCurrentAudio(event.getGuild());
            skipTimer();
            startStage(event.getGuild(), Stage.JUDGES_VERDICT);
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

    private void endDebate() {
        Map<Member, Long> memberToRoleMap = new HashMap<>();
        judges.forEach(jude -> memberToRoleMap.put(jude, RolesID.JUDGE));
        // Проверяем каждого участника на null перед добавлением.
        if (headGovernment != null) memberToRoleMap.put(headGovernment, RolesID.HEAD_GOVERNMENT);
        if (headOpposition != null) memberToRoleMap.put(headOpposition, RolesID.HEAD_OPPOSITION);
        if (memberGovernment != null) memberToRoleMap.put(memberGovernment, RolesID.MEMBER_GOVERNMENT);
        if (memberOpposition != null) memberToRoleMap.put(memberOpposition, RolesID.MEMBER_OPPOSITION);

        useCase.enabledMicrophone(tribuneVoiceChannel.getMembers()).thenAccept(success -> {
            useCase.removeRoleFromUsers(memberToRoleMap).thenAccept(success1 -> {
                useCase.deleteVoiceChannels(allVoiceChannels.stream().toList()).thenAccept(success2 -> {
                    addDebateToDatabase();
                });
            });
        });
    }

    private void addDebateToDatabase() {
        Debate debate = new Debate();

        List<Member> governmentDebaters = new ArrayList<>();
        if (headGovernment != null) governmentDebaters.add(headGovernment);
        if (memberGovernment != null) governmentDebaters.add(memberGovernment);

        List<Member> oppositionDebaters = new ArrayList<>();
        if (headOpposition != null) oppositionDebaters.add(headOpposition);
        if (memberOpposition != null) oppositionDebaters.add(memberOpposition);

        debate.setGovernmentDebaters(governmentDebaters);
        debate.setOppositionDebaters(oppositionDebaters);
        debate.setEndDateTime(LocalDateTime.now());
        debate.setIsGovernmentWinner(winner == Winner.GOVERNMENT);

        useCase.addDebate(debate).thenAccept(success3 -> {
            if (success3) {
                subscribeController.endDebate();
            } else {
                System.out.println("DEBATE NOT ENDED");
            }
        });
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

    private void moveMembers(List<Member> members, VoiceChannel targetChannel, Runnable callback) {
        List<Member> modifiableMembers = new ArrayList<>(members);
        modifiableMembers.remove(bot);
        useCase.moveMembers(modifiableMembers, targetChannel).thenAccept(success -> {
            if (callback != null) callback.run();
        });
    }

    private void enableMicrophone(List<Member> members, Runnable callback) {
        List<Member> modifiableMembers = new ArrayList<>(members);
        modifiableMembers.remove(bot);
        useCase.enabledMicrophone(modifiableMembers).thenAccept(success -> {
            if (callback != null) callback.run();
        });
    }

    private void disableMicrophone(List<Member> members, Runnable callback) {
        List<Member> modifiableMembers = new ArrayList<>(members);
        modifiableMembers.remove(bot);
        useCase.disabledMicrophone(modifiableMembers).thenAccept(success -> {
            if (callback != null) callback.run();
        });
    }

//    private CompletableFuture<List<Member>> getAllDebaters() {
//        CompletableFuture<List<Member>> future = useCase.getMembersByRole(RolesID.HEAD_GOVERNMENT);
//        CompletableFuture<List<Member>> future1 = useCase.getMembersByRole(RolesID.HEAD_OPPOSITION);
//        CompletableFuture<List<Member>> future2 = useCase.getMembersByRole(RolesID.MEMBER_GOVERNMENT);
//        CompletableFuture<List<Member>> future3 = useCase.getMembersByRole(RolesID.MEMBER_OPPOSITION);
//
//        return future.thenCombine(future1, (headGovernment, headOpposition) -> {
//            List<Member> allDebaters = new ArrayList<>();
//            if (headGovernment != null) allDebaters.addAll(headGovernment);
//            if (headOpposition != null) allDebaters.addAll(headOpposition);
//            return allDebaters;
//        }).thenCombine(future2, (allDebaters, memberGovernment) -> {
//            if (memberGovernment != null) allDebaters.addAll(memberGovernment);
//            return allDebaters;
//        }).thenCombine(future3, (allDebaters, memberOpposition) -> {
//            if (memberOpposition != null) allDebaters.addAll(memberOpposition);
//            return allDebaters;
//        });
//    }
//
//    private CompletableFuture<List<Member>> getGovernmentDebaters() {
//        CompletableFuture<List<Member>> future = useCase.getMembersByRole(RolesID.HEAD_GOVERNMENT);
//        CompletableFuture<List<Member>> future1 = useCase.getMembersByRole(RolesID.MEMBER_GOVERNMENT);
//
//        return future.thenCombine(future1, (headGovernment, memberGovernment) -> {
//            List<Member> governmentDebaters = new ArrayList<>();
//            if (headGovernment != null) governmentDebaters.addAll(headGovernment);
//            if (memberGovernment != null) governmentDebaters.addAll(memberGovernment);
//            return governmentDebaters;
//        });
//    }
//
//    private CompletableFuture<List<Member>> getOppositionDebaters() {
//        CompletableFuture<List<Member>> future = useCase.getMembersByRole(RolesID.HEAD_OPPOSITION);
//        CompletableFuture<List<Member>> future1 = useCase.getMembersByRole(RolesID.MEMBER_OPPOSITION);
//
//        return future.thenCombine(future1, (headOpposition, memberOpposition) -> {
//            List<Member> oppositionDebaters = new ArrayList<>();
//            if (headOpposition != null) oppositionDebaters.addAll(headOpposition);
//            if (memberOpposition != null) oppositionDebaters.addAll(memberOpposition);
//            return oppositionDebaters;
//        });
//    }

}
