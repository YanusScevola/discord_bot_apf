package org.example.core.controllers;

import java.awt.Color;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
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
import org.example.core.constants.VoiceChannelsID;
import org.example.core.constants.enums.Stage;
import org.example.core.models.Debate;
import org.example.core.models.Debater;
import org.example.core.models.Theme;
import org.example.domain.UseCase;
import org.example.core.player.PlayerManager;
import org.example.resources.Colors;
import org.example.resources.StringRes;
import org.example.core.constants.RolesID;
import org.example.core.constants.enums.Winner;
import org.example.core.stagetimer.StageTimer;
import org.jetbrains.annotations.NotNull;

public class DebateController {
    private static final int GOVERNMENT_DEBATERS_LIMIT = 1;
    private static final int OPPOSITION_DEBATERS_LIMIT = 0;

    private static final int DEBATERS_PREPARATION_TIME = 15;
    private static final int HEAD_GOVERNMENT_FIRST_SPEECH_TIME = 15;
    private static final int HEAD_OPPOSITION_FIRST_SPEECH_TIME = 15;
    private static final int MEMBER_GOVERNMENT_SPEECH_TIME = 15;
    private static final int MEMBER_OPPOSITION_SPEECH_TIME = 15;
    private static final int OPPONENT_ASK_TIME = 15;
    private static final int HEAD_OPPOSITION_LAST_SPEECH_TIME = 15;
    private static final int HEAD_GOVERNMENT_LAST_SPEECH_TIME = 15;
    private static final int JUDGES_PREPARATION_TIME = 20;
    private static final int WAITING_MEMBER_IN_TRIBUNE_TIME = 15;
    private static final int BEEP_TIME = 5;
    private static final boolean isTest = true;
//
//    private static final int GOVERNMENT_DEBATERS_LIMIT = 2;
//    private static final int OPPOSITION_DEBATERS_LIMIT = 2;
//
//    private static final int DEBATERS_PREPARATION_TIME = 15 * 60;
//    private static final int HEAD_GOVERNMENT_FIRST_SPEECH_TIME = 7 * 60;
//    private static final int HEAD_OPPOSITION_FIRST_SPEECH_TIME = 8 * 60;
//    private static final int MEMBER_GOVERNMENT_SPEECH_TIME = 8 * 60;
//    private static final int MEMBER_OPPOSITION_SPEECH_TIME = 8 * 60;
//    private static final int OPPONENT_ASK_TIME = 20;
//    private static final int HEAD_OPPOSITION_LAST_SPEECH_TIME = 4 * 60;
//    private static final int HEAD_GOVERNMENT_LAST_SPEECH_TIME = 5 * 60;
//    private static final int JUDGES_PREPARATION_TIME = 20 * 60;
//    private static final int WAITING_MEMBER_IN_TRIBUNE_TIME = 60;
//    private static final int BEEP_TIME = 60;
//    private static final boolean isTest = false;

    private static final String END_SPEECH_BTN_ID = "end_speech";
    private static final String ASK_QUESTION_BTN_ID = "ask_question";
    private static final String VOTE_GOVERNMENT_BTN_ID = "vote_government";
    private static final String VOTE_OPPOSITION_BTN_ID = "vote_opposition";

    private final UseCase useCase;
    private SubscribeController subscribeController;

    private VoiceChannel judgesVoiceChannel;
    private VoiceChannel tribuneVoiceChannel;
    private VoiceChannel governmentVoiceChannel;
    private VoiceChannel oppositionVoiceChannel;
    private VoiceChannel waitingRoomVoiceChannel;

    private Member bot;
    private Member headGovernment;
    private Member headOpposition;
    private Member memberGovernment;
    private Member memberOpposition;

    private final Button askQuestionButton;
    private final Button endSpeechButton;
    private final Button voteGovernmentButton;
    private final Button voteOppositionButton;

    private final Set<VoiceChannel> allVoiceChannels = new HashSet<>();
    private final Set<Member> allDebaters = new HashSet<>();
    private final Set<Member> governmentDebaters = new HashSet<>();
    private final Set<Member> oppositionDebaters = new HashSet<>();
    private final Set<Member> judges = new HashSet<>();
    private final List<Member> winners = new ArrayList<>();
    private final Map<Member, Winner> judgeVotes = new HashMap<>();

    private StageTimer currentStageTimer;
    private java.util.TimerTask beepTimerTask;
    private Stage currentStage = Stage.START_DEBATE;
    private Winner winner = Winner.NO_WINNER;
    private Timer waitingMemberInTribuneTimer;
    private Theme currentTheme;
    private Message votingMessage;
    private Message waitingMessage;
    private int votesForGovernment = 0;
    private int votesForOpposition = 0;
    private boolean isDebateStarted = false;
    private boolean isDebateFinished = false;
    private boolean isStartAskOpponent = false;
    private boolean isWaitingMemberInTribuneForSpeak = false;
    private boolean isMemberOfGovernmentAlreadyAsked = false;
    private boolean isMemberOfOppositionAlreadyAsked = false;

    private static final HashMap<Integer, Long> roleIdByWinCountMap = new HashMap<>();

    static {
        roleIdByWinCountMap.put(2, RolesID.DEBATER_APF_2);
        roleIdByWinCountMap.put(4, RolesID.DEBATER_APF_3);
        roleIdByWinCountMap.put(8, RolesID.DEBATER_APF_4);
        roleIdByWinCountMap.put(16, RolesID.DEBATER_APF_5);
    }


    public DebateController(UseCase useCase, SubscribeController subscribeController) {
        this.useCase = useCase;
        this.subscribeController = subscribeController;

        askQuestionButton = Button.primary(ASK_QUESTION_BTN_ID, StringRes.BUTTON_ASK_QUESTION);
        endSpeechButton = Button.primary(END_SPEECH_BTN_ID, StringRes.BUTTON_END_SPEECH);
        voteGovernmentButton = Button.primary(VOTE_GOVERNMENT_BTN_ID, StringRes.BUTTON_VOTE_GOVERNMENT);
        voteOppositionButton = Button.primary(VOTE_OPPOSITION_BTN_ID, StringRes.BUTTON_VOTE_OPPOSITION);
        waitingRoomVoiceChannel = useCase.getVoiceChannel(VoiceChannelsID.WAITING_ROOM).join();
    }

    public void onJoinToTribuneVoiceChannel(Guild guild, AudioChannel channelJoined, Member member) {
        initDebateMember(guild, member);
        boolean isBotJoined = member.equals(bot);

        if (currentStage != Stage.DEBATERS_PREPARATION && currentStage != Stage.JUDGES_PREPARATION) {
            disableMicrophone(Arrays.asList(member), null);
        }

        if (isDebateStarted) {
            handelWaitingMemberInTribuneForSpeak(guild, channelJoined, member);
        }

        if (currentStage == Stage.JUDGES_PREPARATION) {
            if (new HashSet<>(judgesVoiceChannel.getMembers()).containsAll(judges)) {
                startStage(guild, Stage.JUDGES_VERDICT);
            }
        }

        if (isBotJoined && !isDebateStarted) {
            isDebateStarted = true;
            moveMembers(waitingRoomVoiceChannel.getMembers(), tribuneVoiceChannel, () -> {
                startDebate(guild);
            });
        }
    }

    public void onLeaveFromTribuneVoiceChannel(Guild guild, AudioChannel channelLeft, Member member) {
        if (!isDebateFinished) useCase.enabledMicrophone(Arrays.asList(member));

        if (isDebateFinished && tribuneVoiceChannel.getMembers().isEmpty()) {
            useCase.deleteVoiceChannels(Arrays.asList(tribuneVoiceChannel)).join();
        }
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String buttonId = Objects.requireNonNull(event.getButton().getId());
        switch (buttonId) {
            case END_SPEECH_BTN_ID:
                onClickEndSpeechBtn(event);
                break;
            case ASK_QUESTION_BTN_ID:
                onClickAskQuestionBtn(event);
                break;
            case VOTE_GOVERNMENT_BTN_ID:
                onClickVoteGovernmentBtn(event);
                break;
            case VOTE_OPPOSITION_BTN_ID:
                onClickVoteOppositionBtn(event);
                break;
            default:
                useCase.showEphemeralShortLoading(event).thenAccept(message -> {
                    message.editOriginal("Кнопка не найдена").queue();
                });
                break;
        }
    }


    public void addChannel(VoiceChannel channel) {
        String voiceChannelName = channel.getName();

        if (voiceChannelName.equals(StringRes.CHANNEL_TRIBUNE))
            tribuneVoiceChannel = channel;
        if (voiceChannelName.equals(StringRes.CHANNEL_JUDGE))
            judgesVoiceChannel = channel;
        if (voiceChannelName.equals(StringRes.CHANNEL_GOVERNMENT))
            governmentVoiceChannel = channel;
        if (voiceChannelName.equals(StringRes.CHANNEL_OPPOSITION))
            oppositionVoiceChannel = channel;

        allVoiceChannels.add(channel);
    }

    private void onClickAskQuestionBtn(ButtonInteractionEvent event) {
        useCase.showEphemeralShortLoading(event).thenAccept(message -> {
            if (allDebaters.contains(event.getMember())) {
                if (currentStage == Stage.MEMBER_GOVERNMENT_SPEECH) {
                    if (oppositionDebaters.contains(event.getMember())) {
                        if (isMemberOfGovernmentAlreadyAsked) {
                            message.editOriginal("Вы уже не можете задать вопрос").queue();
                            return;
                        }
                        message.editOriginal(StringRes.REMARK_ASK_GOVERNMENT_MEMBER).queue((msg) -> {
                            isMemberOfGovernmentAlreadyAsked = true;
                            startAskOpponent(event.getGuild(), event.getMember(), memberGovernment);
                        });
                    } else
                        message.editOriginal(StringRes.WARNING_NOT_ASK_OWN_TEAM).queue();
                } else if (currentStage == Stage.MEMBER_OPPOSITION_SPEECH) {
                    if (governmentDebaters.contains(event.getMember())) {
                        if (isMemberOfOppositionAlreadyAsked) {
                            message.editOriginal("Вы уже не можете задать вопрос").queue();
                            return;
                        }
                        message.editOriginal(StringRes.REMARK_ASK_OPPOSITION_MEMBER).queue((msg) -> {
                            isMemberOfOppositionAlreadyAsked = true;
                            startAskOpponent(event.getGuild(), event.getMember(), memberGovernment);
                        });
                    } else
                        message.editOriginal(StringRes.WARNING_NOT_ASK_OWN_TEAM).queue();
                } else message.editOriginal("Сейчас нельзя задавать вопросы").queue();
            } else message.editOriginal(StringRes.WARNING_NOT_DEBATER).queue();
        });
    }

    private void onClickEndSpeechBtn(ButtonInteractionEvent event) {
        useCase.showEphemeralShortLoading(event).thenAccept(message -> {
            Member member = event.getMember();
            boolean canEndSpeech = (currentStage == Stage.HEAD_GOVERNMENT_FIRST_SPEECH && headGovernment.equals(member)) ||
                    (currentStage == Stage.HEAD_OPPOSITION_FIRST_SPEECH && headOpposition.equals(member)) ||
                    (currentStage == Stage.MEMBER_GOVERNMENT_SPEECH && memberGovernment.equals(member)) ||
                    (currentStage == Stage.MEMBER_OPPOSITION_SPEECH && memberOpposition.equals(member)) ||
                    (currentStage == Stage.HEAD_GOVERNMENT_LAST_SPEECH && headGovernment.equals(member)) ||
                    (currentStage == Stage.HEAD_OPPOSITION_LAST_SPEECH && headOpposition.equals(member));

            if (canEndSpeech) {
                message.editOriginal(StringRes.REMARK_SPEECH_END).queue((msg) -> {
                    skipTimer(member.getId());
                });
            } else {
                message.editOriginal("Вы не можете закончить чужую речь").queue();
            }
        });
    }

    private void onClickVoteGovernmentBtn(ButtonInteractionEvent event) {
        useCase.showEphemeralShortLoading(event).thenAccept(message -> {
            if (judges.contains(event.getMember())) {
                voteForWinner(event, Winner.GOVERNMENT);
//                voteForWinner(event, Winner.GOVERNMENT, () -> {
//                    message.editOriginal(StringRes.REMARK_VOTE_GOVERNMENT).queue();
//                });
            } else {
                message.editOriginal(StringRes.WARNING_NOT_JUDGE).queue();
            }
        });
    }

    private void onClickVoteOppositionBtn(ButtonInteractionEvent event) {
        useCase.showEphemeralShortLoading(event).thenAccept(message -> {
            if (judges.contains(event.getMember())) {
                voteForWinner(event, Winner.OPPOSITION);
//                voteForWinner(event, Winner.OPPOSITION, () -> {
//                    message.editOriginal(StringRes.REMARK_VOTE_OPPOSITION).queue();
//                });
            } else {
                message.editOriginal(StringRes.WARNING_NOT_JUDGE).queue();
            }
        });
    }

    private void initDebateMember(Guild guild, Member member) {
        List<Role> roles = member.getRoles();

        if (roles.contains(guild.getRoleById(RolesID.HEAD_GOVERNMENT))) {
            headGovernment = member;
            governmentDebaters.add(member);
            allDebaters.add(member);
        }
        if (roles.contains(guild.getRoleById(RolesID.HEAD_OPPOSITION))) {
            headOpposition = member;
            oppositionDebaters.add(member);
            allDebaters.add(member);
        }
        if (roles.contains(guild.getRoleById(RolesID.MEMBER_GOVERNMENT))) {
            memberGovernment = member;
            governmentDebaters.add(member);
            allDebaters.add(member);
        }
        if (roles.contains(guild.getRoleById(RolesID.MEMBER_OPPOSITION))) {
            memberOpposition = member;
            oppositionDebaters.add(member);
            allDebaters.add(member);
        }
        if (roles.contains(guild.getRoleById(RolesID.JUDGE))) {
            judges.add(member);
        }
        if (member.equals(guild.getSelfMember())) {
            bot = member;
        }

        if (isTest) {
            if (judges.size() != 0)
                headOpposition = new ArrayList<>(judges).get(0);
            memberGovernment = headGovernment;
            memberOpposition = headGovernment;
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
        isDebateFinished = false;
        startStage(guild, Stage.START_DEBATE);
    }

    private void sendDebateTheme() {
        useCase.getRandomTheme().thenAccept(theme -> {
            currentTheme = theme;
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Тема: " + currentTheme.getName()).setColor(Colors.GREEN);
            tribuneVoiceChannel.sendMessageEmbeds(embedBuilder.build()).queue();
        });
    }

    private void startStage(Guild guild, @NotNull Stage stage) {
        switch (stage) {
            case START_DEBATE:
                startGreetingsStage(guild);
                break;
            case DEBATERS_PREPARATION:
                startDebatersPreparationStage(guild);
                break;
            case HEAD_GOVERNMENT_FIRST_SPEECH:
                startHeadGovernmentFirstSpeechStage(guild);
                break;
            case HEAD_OPPOSITION_FIRST_SPEECH:
                startHeadOppositionFirstSpeechStage(guild);
                break;
            case MEMBER_GOVERNMENT_SPEECH:
                startMemberGovernmentSpeechStage(guild);
                break;
            case MEMBER_OPPOSITION_SPEECH:
                startMemberOppositionSpeechStage(guild);
                break;
            case HEAD_OPPOSITION_LAST_SPEECH:
                startHeadOppositionLastSpeechStage(guild);
                break;
            case HEAD_GOVERNMENT_LAST_SPEECH:
                startHeadGovernmentLastSpeechStage(guild);
                break;
            case JUDGES_PREPARATION:
                startJudgesPreparationStage(guild);
                break;
            case JUDGES_VERDICT:
                startVerdictStage(guild);
                break;
            default:
                // Обработка неожиданных значений перечисления, если необходимо
                break;
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
        String title = StringRes.TITLE_DEBATER_PREPARATION;
        List<Button> buttons = Arrays.asList();

        playAudio(guild, "Подготовка дебатеров.mp3", () -> {
            sendDebateTheme();
            moveMembers(new ArrayList<>(oppositionDebaters), oppositionVoiceChannel, () -> {
                moveMembers(new ArrayList<>(governmentDebaters), governmentVoiceChannel, () -> {
                    startTimer(currentStageTimer, title, "", DEBATERS_PREPARATION_TIME, buttons, guild, () -> {
                        ArrayList<Member> allDebaterList = new ArrayList<>();
                        allDebaterList.addAll(tribuneVoiceChannel.getMembers());
                        allDebaterList.addAll(governmentVoiceChannel.getMembers());
                        allDebaterList.addAll(oppositionVoiceChannel.getMembers());
                        disableMicrophone(allDebaterList, () -> {
                            moveMembers(new ArrayList<>(allDebaters), tribuneVoiceChannel, () -> {
                                stopCurrentAudio(guild);
                                startStage(guild, Stage.HEAD_GOVERNMENT_FIRST_SPEECH);
                            });
                        });
                    });
                    playAudio(guild, "Разговоры пока дебатеры готовятся.mp3", () -> {
                        enableMicrophone(tribuneVoiceChannel.getMembers(), null);
                        playAudio(guild, "Тишина.mp3", null);
                    });
                });
            });
        });
    }

    private void startHeadGovernmentFirstSpeechStage(Guild guild) {
        boolean isDebaterAreInTribune = tribuneVoiceChannel.getMembers().contains(headGovernment);
        currentStage = Stage.HEAD_GOVERNMENT_FIRST_SPEECH;
        currentStageTimer = new StageTimer(tribuneVoiceChannel);
        String title = StringRes.TITLE_HEAD_GOVERNMENT_FIRST_SPEECH;
        List<Button> buttons = Collections.singletonList(endSpeechButton);

        if (isDebaterAreInTribune || isTest) {
            playAudio(guild, "Вступ глава правительства.mp3", () -> {
                enableMicrophone(Collections.singletonList(headGovernment), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, headGovernment.getId(), HEAD_GOVERNMENT_FIRST_SPEECH_TIME, buttons, guild, () -> {
                        disableMicrophone(Collections.singletonList(headGovernment), () -> {
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
        String title = StringRes.TITLE_HEAD_OPPOSITION_FIRST_SPEECH;
        List<Button> buttons = Arrays.asList(endSpeechButton);

        if (isDebaterAreInTribune || isTest) {
            playAudio(guild, "Вступ глава оппозиции.mp3", () -> {
                enableMicrophone(Arrays.asList(headOpposition), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, headOpposition.getId(), HEAD_OPPOSITION_FIRST_SPEECH_TIME, buttons, guild, () -> {
                        disableMicrophone(Arrays.asList(headOpposition), () -> {
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
        String title = StringRes.TITLE_MEMBER_GOVERNMENT_SPEECH;
        List<Button> buttons = Arrays.asList(askQuestionButton, endSpeechButton);
        if (isDebaterAreInTribune || isTest) {
            playAudio(guild, "Член правительства.mp3", () -> {
                enableMicrophone(Arrays.asList(memberGovernment), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, memberGovernment.getId(), MEMBER_GOVERNMENT_SPEECH_TIME, buttons, guild, () -> {
                        disableMicrophone(Arrays.asList(memberGovernment), () -> {
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
        String title = StringRes.TITLE_MEMBER_OPPOSITION_SPEECH;
        List<Button> buttons = Arrays.asList(askQuestionButton, endSpeechButton);
        if (isDebaterAreInTribune || isTest) {
            playAudio(guild, "Член оппозиции.mp3", () -> {
                enableMicrophone(Arrays.asList(memberOpposition), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, memberOpposition.getId(), MEMBER_OPPOSITION_SPEECH_TIME, buttons, guild, () -> {
                        disableMicrophone(Arrays.asList(memberOpposition), () -> {
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
        String title = StringRes.TITLE_HEAD_OPPOSITION_LAST_SPEECH;
        List<Button> buttons = Arrays.asList(endSpeechButton);
        if (isDebaterAreInTribune || isTest) {
            playAudio(guild, "Закл глава оппозиции.mp3", () -> {
                enableMicrophone(Arrays.asList(headOpposition), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, headOpposition.getId(), HEAD_OPPOSITION_LAST_SPEECH_TIME, buttons, guild, () -> {
                        disableMicrophone(Arrays.asList(headOpposition), () -> {
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
        String title = StringRes.TITLE_HEAD_GOVERNMENT_LAST_SPEECH;
        List<Button> buttons = Arrays.asList(endSpeechButton);
        if (isDebaterAreInTribune || isTest) {
            playAudio(guild, "Закл глава правительства.mp3", () -> {
                enableMicrophone(Arrays.asList(headGovernment), () -> {
                    playAudio(guild, "Тишина.mp3", null);
                    startTimer(currentStageTimer, title, headGovernment.getId(), HEAD_GOVERNMENT_LAST_SPEECH_TIME, buttons, guild, () -> {
                        disableMicrophone(Arrays.asList(headGovernment), () -> {
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
        String title = StringRes.TITLE_JUDGES_PREPARATION;
        playAudio(guild, "Подготовка судей.mp3", () -> {
//            playAudio(guild, "Разговоры пока судьи готовятся.mp3", () -> {
            moveMembers(judges.stream().collect(Collectors.toList()), judgesVoiceChannel, () -> {
                sendVotingMessage();
                startTimer(currentStageTimer, title, "", JUDGES_PREPARATION_TIME, new ArrayList<>(), guild, () -> {
//                    if (winners == null || winners.isEmpty()) {
                    disableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                        stopCurrentAudio(guild);
                        moveMembers(judges.stream().collect(Collectors.toList()), tribuneVoiceChannel, () -> {
                            startStage(guild, Stage.JUDGES_VERDICT);
                        });
                    });
//                    }
                });
                enableMicrophone(tribuneVoiceChannel.getMembers(), null);
                playAudio(guild, "Тишина.mp3", () -> {

                });
//                });
            });
        });
    }

    private void startAskOpponent(Guild guild, Member asker, Member answerer) {
        stopCurrentAudio(guild);
        isStartAskOpponent = true;
        disableMicrophone(Arrays.asList(answerer), () -> {
            pauseTimer(OPPONENT_ASK_TIME, 6, () -> {
                isStartAskOpponent = false;
                playAudio(guild, "Тишина.mp3", null);
                disableMicrophone(Arrays.asList(asker), () -> {
                    enableMicrophone(Arrays.asList(answerer), this::resumeTimer);
                });
            });
            playAudio(guild, "Опонент задает вопрос.mp3", () -> {
                enableMicrophone(Arrays.asList(asker), null);
            });
        });
    }

    private void startVerdictStage(Guild guild) {
        currentStage = Stage.JUDGES_VERDICT;
        System.out.println("Голосование завершено");
        moveMembers(judges.stream().collect(Collectors.toList()), tribuneVoiceChannel, () -> {
            disableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                switch (winner) {
                    case GOVERNMENT:
                        playAudio(guild, "Победа правительства.mp3", () -> {
                            enableMicrophone(judges.stream().collect(Collectors.toList()), () -> {
                                AudioManager audioManager = guild.getAudioManager();
                                if (audioManager.isConnected()) {
                                    audioManager.closeAudioConnection();
                                    System.out.println("Остановка аудио");
                                    endDebate();
                                } else {
                                    System.out.println("Бот уже не находится в голосовом канале.");
                                }
                            });
                        });
                        break;
                    case OPPOSITION:
                        playAudio(guild, "Победа оппозиции.mp3", () -> {
                            enableMicrophone(judges.stream().collect(Collectors.toList()), () -> {
                                AudioManager audioManager = guild.getAudioManager();
                                if (audioManager.isConnected()) {
                                    audioManager.closeAudioConnection();
                                    endDebate();
                                } else {
                                    System.out.println("Бот уже не находится в голосовом канале.");
                                }
                            });
                        });
                        break;
                    case NO_WINNER:
                        playAudio(guild, "Ничья.mp3", () -> {
                            enableMicrophone(tribuneVoiceChannel.getMembers(), () -> {
                                AudioManager audioManager = guild.getAudioManager();
                                if (audioManager.isConnected()) {
                                    audioManager.closeAudioConnection();
                                    endDebateWithOutWinner();
                                } else {
                                    System.out.println("Бот уже не находится в голосовом канале.");
                                }
                            });
                        });
                        break;
                    default:
                        // Действие в случае, если winner не соответствует ни одному из ожидаемых значений
                        break;
                }
            });
        });
        isDebateFinished = true;
    }


    private void waitForMemberInTribune(Guild guild, Member member, long delayInSeconds, String title) {
        long currentTimeMillis = System.currentTimeMillis();
        String timerText = "Таймер: <t:" + (currentTimeMillis / 1000 + delayInSeconds) + ":R>";

        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle(title + " отсутствует в трибуне, предлагаю подождать его подождать.");
        embed.setDescription(timerText);
        embed.setColor(Colors.YELLOW);

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
                            endDebateWithOutWinner();
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
        embedBuilder.setColor(Colors.YELLOW);
        message.editMessageEmbeds(embedBuilder.build()).queue();
    }

    public void sendVotingMessage() {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(StringRes.TITLE_VOTING);
        eb.setDescription(StringRes.DESCRIPTION_VOTE_FOR_GOVERNMENT + votesForGovernment + StringRes.DESCRIPTION_VOTE_FOR_OPPOSITION + votesForOpposition);
        eb.setColor(Colors.BLUE);

        judgesVoiceChannel.sendMessageEmbeds(eb.build())
                .setActionRow(voteGovernmentButton, voteOppositionButton)
                .queue(message -> votingMessage = message);
    }

    private void voteForWinner(ButtonInteractionEvent event, Winner newVote) {
        Member judge = event.getMember();
        if (!judges.contains(judge)) {
            if (!event.isAcknowledged()) {
                event.reply(StringRes.WARNING_NOT_JUDGE).setEphemeral(true).queue();
            }
            return;
        }

        Winner previousVote = judgeVotes.get(judge);
        if (previousVote != null) {
            if (previousVote == newVote) {
                if (newVote == Winner.GOVERNMENT) {
                    votesForGovernment--;
                } else {
                    votesForOpposition--;
                }
                judgeVotes.remove(judge);
                updateVotingMessage();
                if (!event.isAcknowledged()) {
                    event.reply("Вы отменили свой голос.").setEphemeral(true).queue();
                } else {
                    event.getHook().editOriginal("Вы отменили свой голос.").queue();
                }
                return;
            } else {
                if (previousVote == Winner.GOVERNMENT) {
                    votesForGovernment--;
                } else {
                    votesForOpposition--;
                }
            }
        }

        judgeVotes.put(judge, newVote);
        if (newVote == Winner.GOVERNMENT) {
            votesForGovernment++;
        } else {
            votesForOpposition++;
        }
        updateVotingMessage();

        if (!event.isAcknowledged()) {
            event.reply("Ваш голос за " + (newVote == Winner.GOVERNMENT ? "правительство" : "оппозицию") + " учтён.").setEphemeral(true).queue();
        } else {
            event.getHook().editOriginal("Ваш голос за " + (newVote == Winner.GOVERNMENT ? "правительство" : "оппозицию") + " учтён.").queue();
        }

        if (judgeVotes.size() == judges.size() && judgeVotes.values().stream().noneMatch(Objects::isNull)) {
            if (votesForGovernment > votesForOpposition) {
                winner = Winner.GOVERNMENT;
                winners.clear();
                winners.addAll(governmentDebaters);
            } else if (votesForOpposition > votesForGovernment) {
                winner = Winner.OPPOSITION;
                winners.clear();
                winners.addAll(oppositionDebaters);
            } else {
                winner = Winner.NO_WINNER;
                winners.clear();
            }

            disableVotingButtons();
            skipTimer("");
        }
    }


    private void updateVotingMessage() {
        if (votingMessage == null) {
            return;
        }
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Голосование:");
        eb.setDescription("За правительство: " + votesForGovernment + "\nЗа оппозицию: " + votesForOpposition);
        eb.setColor(Colors.BLUE);
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
    }

    private void endDebate() {
        try {
            Map<Member, Long> memberToRoleMap = new HashMap<>();
            List<Member> allDebatersEndJudges = new ArrayList<>();
            allDebatersEndJudges.addAll(allDebaters);
            allDebatersEndJudges.addAll(judges);

            judges.forEach(jude -> memberToRoleMap.put(jude, RolesID.JUDGE));
            if (headGovernment != null) memberToRoleMap.put(headGovernment, RolesID.HEAD_GOVERNMENT);
            if (headOpposition != null) memberToRoleMap.put(headOpposition, RolesID.HEAD_OPPOSITION);
            if (memberGovernment != null) memberToRoleMap.put(memberGovernment, RolesID.MEMBER_GOVERNMENT);
            if (memberOpposition != null) memberToRoleMap.put(memberOpposition, RolesID.MEMBER_OPPOSITION);

            useCase.removeRoleFromUsers(memberToRoleMap);
            useCase.deleteVoiceChannels(Arrays.asList(governmentVoiceChannel, oppositionVoiceChannel, judgesVoiceChannel));
            insertDebaterEndDebateToDb(allDebatersEndJudges.stream().collect(Collectors.toList()), () -> {
                System.out.println("END DEBATE");
                judgeVotes.clear();
                subscribeController.endDebate();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void endDebateWithOutWinner() {
        try {
            Map<Member, Long> memberToRoleMap = new HashMap<>();

            judges.forEach(jude -> memberToRoleMap.put(jude, RolesID.JUDGE));
            if (headGovernment != null) memberToRoleMap.put(headGovernment, RolesID.HEAD_GOVERNMENT);
            if (headOpposition != null) memberToRoleMap.put(headOpposition, RolesID.HEAD_OPPOSITION);
            if (memberGovernment != null) memberToRoleMap.put(memberGovernment, RolesID.MEMBER_GOVERNMENT);
            if (memberOpposition != null) memberToRoleMap.put(memberOpposition, RolesID.MEMBER_OPPOSITION);

            useCase.removeRoleFromUsers(memberToRoleMap).thenAccept(success1 -> {
                useCase.deleteVoiceChannels(Arrays.asList(governmentVoiceChannel, oppositionVoiceChannel, judgesVoiceChannel));
                System.out.println("END DEBATE");
                judgeVotes.clear();
                subscribeController.endDebateWithOutWinner();
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void insertDebaterEndDebateToDb(List<Member> members, Runnable callback) {
        Debate finishedDebate = getFinishedDebate();
        List<Long> memberIds = members.stream().map(Member::getIdLong).collect(Collectors.toList());
//        useCase.getRandomTheme().thenAccept(theme -> {
//            currentTheme = theme;
        currentTheme.setUsageCount(currentTheme.getUsageCount() + 1);
        finishedDebate.setTheme(currentTheme);
        useCase.addDebate(finishedDebate).thenAccept(resultDebate -> {
            finishedDebate.setId(resultDebate.getId());
            useCase.getDebatersByMemberId(memberIds).thenAccept(allDebatersByMember -> {
                List<Long> allDebatersIds = allDebatersByMember.stream().map(Debater::getMemberId).collect(Collectors.toList());
                List<Debater> finishedDebaters = new ArrayList<>();

                for (Member member : members) {
                    if (judges.contains(member)) continue;
                    if (allDebatersIds.contains(member.getIdLong())) {
                        Debater debater = allDebatersByMember.stream().filter(d -> d.getMemberId() == member.getIdLong()).findFirst().orElse(null);
                        if (winners.contains(member)) {
                            debater.setWinnCount(debater.getWinnCount() + 1);
                            if (roleIdByWinCountMap.containsKey(debater.getWinnCount())) {
                                List<Long> allRolesForDebater = roleIdByWinCountMap.values().stream().collect(Collectors.toList());
                                List<Role> currentMemberRoles = member.getRoles();
                                Long newDebaterRole = roleIdByWinCountMap.get(debater.getWinnCount());
                                allRolesForDebater.forEach(debaterRoleId -> {
                                    if (currentMemberRoles.stream().anyMatch(role -> role.getIdLong() == debaterRoleId)) {
                                        Map<Member, Long> rolesToRemove = new HashMap<>();
                                        rolesToRemove.put(member, debaterRoleId);
                                        useCase.removeRoleFromUsers(rolesToRemove).thenAccept(success -> {
                                            if (success) {
                                                Map<Member, Long> rolesToAdd = new HashMap<>();
                                                rolesToAdd.put(member, newDebaterRole);
                                                useCase.addRoleToMembers(rolesToAdd);
                                            }
                                        });

                                    }
                                });
                            }

                        } else {
                            debater.setLossesCount(debater.getLossesCount() + 1);
                        }
                        debater.setNickname(member.getUser().getName());
                        debater.setServerNickname(member.getEffectiveName());
                        debater.getDebates().add(finishedDebate);
                        finishedDebaters.add(debater);
                    } else {
                        Debater debater = new Debater();
                        debater.setMemberId(member.getIdLong());
                        debater.setNickname(member.getUser().getName());
                        debater.setServerNickname(member.getEffectiveName());
                        if (winners.contains(member)) {
                            debater.setWinnCount(1);
                        } else {
                            debater.setLossesCount(1);
                        }
                        debater.setDebates(Arrays.asList(finishedDebate));
                        finishedDebaters.add(debater);
                    }
                }

                useCase.addDebaters(finishedDebaters).thenAccept(success4 -> {
                    if (success4) {
                        if (callback != null) callback.run();
                    } else {
                        System.out.println("DEBATERS NOT ADDED");
                    }
                });
            });
//            });
        });
    }

    private Debate getFinishedDebate() {
        Debate debate = new Debate();

        List<Member> governmentDebaters = new ArrayList<>();
        if (headGovernment != null) governmentDebaters.add(headGovernment);
        if (memberGovernment != null) governmentDebaters.add(memberGovernment);

        List<Member> judgeList = new ArrayList<>(this.judges);

        List<Member> oppositionDebaters = new ArrayList<>();
        if (headOpposition != null) oppositionDebaters.add(headOpposition);
        if (memberOpposition != null) oppositionDebaters.add(memberOpposition);

        debate.setGovernmentDebaters(governmentDebaters);
        debate.setOppositionDebaters(oppositionDebaters);
        debate.setJudges(judgeList);
        debate.setEndDateTime(LocalDateTime.now());
        debate.setIsGovernmentWinner(winner == Winner.GOVERNMENT);
        return debate;
    }

    private void playAudio(Guild guild, String path, Runnable callback) {
        System.out.println("PLAY " + path);
        String AUDIO_BASE_PATH = "src/main/resources/audio/";
        guild.getAudioManager().setSendingHandler(PlayerManager.get().getAudioSendHandler());
        PlayerManager.get().play(guild, AUDIO_BASE_PATH + path, callback);
    }

    private void stopCurrentAudio(Guild guild) {
        // First check if there is a currently playing track
        AudioPlayer player = PlayerManager.get().getGuildMusicManager(guild).getPlayer();
        AudioTrack currentTrack = player.getPlayingTrack();

        if (currentTrack != null) {
            System.out.println("STOP " + currentTrack.getInfo().title);
        } else {
            System.out.println("STOP attempted, but no track is currently playing.");
        }

        // Stop the player regardless of whether a track is playing
        PlayerManager.get().stop(guild);
    }


    private void startTimer(StageTimer timer, String title, String userId, long time, List<Button> buttons, Guild guild, Runnable timerCallback) {
        System.out.println("START " + title);
        timer.start(title, userId, time, buttons, timerCallback);

        if (time > BEEP_TIME) {
            beepTimerTask = new java.util.TimerTask() {
                @Override
                public void run() {
                    stopCurrentAudio(guild);
                    playAudio(guild, "бип.mp3", null);
                }
            };
            new java.util.Timer().schedule(beepTimerTask, (time - BEEP_TIME) * 1000); // Запускаем таймер на время, уменьшенное на BEEP_TIME
        }
    }


    private void skipTimer(String userId) {
        System.out.println("SKIP");

        if (!isStartAskOpponent) {
            if (beepTimerTask != null) {
                beepTimerTask.cancel();  // Отменяем таймер бипа
                beepTimerTask = null;   // Обнуляем ссылку
            }
        }

        currentStageTimer.skip(userId);
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
        if (modifiableMembers.isEmpty() || modifiableMembers.contains(null)) {
            if (callback != null) callback.run();
            return;
        }
        useCase.moveMembers(modifiableMembers, targetChannel).thenAccept(success -> {
            if (callback != null) callback.run();
        });
    }

    private void enableMicrophone(List<Member> members, Runnable callback) {
        List<Member> modifiableMembers = new ArrayList<>(members);
        modifiableMembers.remove(bot);
        if (modifiableMembers.isEmpty() || modifiableMembers.contains(null)) {
            if (callback != null) callback.run();
            return;
        }
        useCase.enabledMicrophone(modifiableMembers).thenAccept(success -> {
            if (callback != null) callback.run();
        });
    }

    private void disableMicrophone(List<Member> members, Runnable callback) {
        List<Member> modifiableMembers = new ArrayList<>(members);
        modifiableMembers.remove(bot);

        if (modifiableMembers.isEmpty() || modifiableMembers.contains(null)) {
            if (callback != null) callback.run();
            return;
        }
        useCase.disabledMicrophone(modifiableMembers).thenAccept(success -> {
            if (callback != null) callback.run();
        });
    }

}
