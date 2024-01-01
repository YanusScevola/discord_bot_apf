package org.example.ui.channels;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import org.example.Stage;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.lavaplayer.PlayerManager;
import org.example.resources.StringRes;
import org.example.ui.constants.RolesID;
import org.example.ui.models.StageInfo;
import org.example.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class DebateController {
    private static final int GOVERNMENT_DEBATERS_LIMIT = 1;
    private static final int OPPOSITION_DEBATERS_LIMIT = 0;

    private static final int GREETING_TIME = 5;
    private static final int DEBATERS_PREPARATION_TIME = 5;
    private static final int CONVERSATION_DEBATERS_PREPARATION_TIME = 5;
    private static final int HEAD_GOVERNMENT_FIRST_SPEECH_TIME = 5;
    private static final int HEAD_OPPOSITION_FIRST_SPEECH_TIME = 5;
    private static final int MEMBER_GOVERNMENT_SPEECH_TIME = 5;
    private static final int MEMBER_OPPOSITION_SPEECH_TIME = 5;
    private static final int HEAD_OPPOSITION_LAST_SPEECH_TIME = 5;
    private static final int HEAD_GOVERNMENT_LAST_SPEECH_TIME = 5;
    private static final int JUDGES_PREPARATION_TIME = 5;

    private static final String AUDIO_RESOURCE_PATH = "src/main/resources/audio/";

    private VoiceChannel judgesVoiceChannel;
    private VoiceChannel tribuneVoiceChannel;
    private VoiceChannel governmentVoiceChannel;
    private VoiceChannel oppositionVoiceChannel;

    private final ApiRepository apiRepository;
    private final DbRepository dbRepository;
    private final StringRes stringsRes;

    private final Set<Member> allDebaters = new HashSet<>();
    private final Set<Member> governmentDebaters = new HashSet<>();
    private final Set<Member> oppositionDebaters = new HashSet<>();
    private final Set<Member> judges = new HashSet<>();

    private boolean isDebateStarted = false;


    private StageInfo greetingStage;
    private StageInfo debatersPreparationStage;
    private StageInfo conversationDebatersPrepareStage;
    private StageInfo headGovernmentFirstStage;
    private StageInfo headOppositionFirstStage;
    private StageInfo memberGovernmentStage;
    private StageInfo memberOppositionStage;
    private StageInfo headOppositionLastStage;
    private StageInfo headGovernmentLastStage;
    private StageInfo judgesPreparationStage;


    public DebateController(ApiRepository apiRepository, DbRepository dbRepository
                           ) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        this.stringsRes = StringRes.getInstance(StringRes.Language.RUSSIAN);

        initStages();
    }

    private void initStages() {
        greetingStage = new StageInfo(0, "Приветствие.mp3", Stage.DEBATERS_PREPARATION); //0
        debatersPreparationStage = new StageInfo(1, "Подготовка дебатеров.mp3", Stage.HEAD_GOVERNMENT_FIRST_SPEECH); //1
        conversationDebatersPrepareStage = new StageInfo(5, "Разговоры пока дебатеры готовятся.mp3", Stage.HEAD_GOVERNMENT_FIRST_SPEECH); //15
        headGovernmentFirstStage = new StageInfo(5, "Вступ глава правительства.mp3", Stage.HEAD_OPPOSITION_FIRST_SPEECH);
        headOppositionFirstStage = new StageInfo(5, "Вступ глава оппозиции.mp3", Stage.MEMBER_GOVERNMENT_SPEECH);
        memberGovernmentStage = new StageInfo(5, "Член правительства.mp3", Stage.MEMBER_OPPOSITION_SPEECH);
        memberOppositionStage = new StageInfo(5, "Член оппозиции.mp3", Stage.HEAD_OPPOSITION_LAST_SPEECH);
        headOppositionLastStage = new StageInfo(5, "Закл глава оппозиции.mp3", Stage.HEAD_GOVERNMENT_LAST_SPEECH);
        headGovernmentLastStage = new StageInfo(5, "Закл глава правительства.mp3", Stage.JUDGES_PREPARATION);
        judgesPreparationStage = new StageInfo(5, "Подготовка судей.mp3", Stage.HEAD_GOVERNMENT_FIRST_SPEECH);

    }

    public void addChannel(VoiceChannel channel) {
        if (channel.getName().equals(stringsRes.get(StringRes.Key.TRIBUNE_CHANNEL_NAME))) tribuneVoiceChannel = channel;
        if (channel.getName().equals(stringsRes.get(StringRes.Key.JUDGE_CHANNEL_NAME))) judgesVoiceChannel = channel;
        if (channel.getName().equals(stringsRes.get(StringRes.Key.GOVERNMENT_CHANNEL_NAME)))
            governmentVoiceChannel = channel;
        if (channel.getName().equals(stringsRes.get(StringRes.Key.OPPOSITION_CHANNEL_NAME)))
            oppositionVoiceChannel = channel;
    }

    public void onJoinToVoiceChannel(@NotNull GuildVoiceUpdateEvent event) {
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

        boolean isGovernmentDebatersReady = governmentDebaters.size() == GOVERNMENT_DEBATERS_LIMIT;
        boolean isOppositionDebatersReady = oppositionDebaters.size() == OPPOSITION_DEBATERS_LIMIT;
        boolean isJudgesReady = judges.size() >= 1;

        if (isGovernmentDebatersReady && isOppositionDebatersReady && isJudgesReady && !isDebateStarted) {
            isDebateStarted = true;
            startStage(event.getMember().getGuild(), Stage.START_DEBATE);
        }
    }

    public void onLeaveFromVoiceChannel(GuildVoiceUpdateEvent event) {
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

    }

    private void startStage(Guild guild, Stage stage) {
        switch (stage) {
            case START_DEBATE -> startDebateStage(guild);
            case DEBATERS_PREPARATION -> startDebatersPreparationStage(guild);
            case CONVERSATION_DEBATERS_PREPARATION -> startConversationDebatersPrepareStage(guild);
            case HEAD_GOVERNMENT_FIRST_SPEECH -> startHeadGovernmentFirstSpeechStage(guild);
            case HEAD_OPPOSITION_FIRST_SPEECH -> startHeadOppositionFirstSpeechStage(guild);
            case MEMBER_GOVERNMENT_SPEECH -> startMemberGovernmentSpeechStage(guild);
            case MEMBER_OPPOSITION_SPEECH -> startMemberOppositionSpeechStage(guild);
            case HEAD_OPPOSITION_LAST_SPEECH -> startHeadOppositionLastSpeechStage(guild);
            case HEAD_GOVERNMENT_LAST_SPEECH -> startHeadGovernmentLastSpeechStage(guild);
            case JUDGES_PREPARATION -> startJudgesPreparationStage(guild);
        }
    }

    private void startDebateStage(Guild guild) {
        isDebateStarted = true;
        greetingStage.play(guild, () -> {
            startStage(guild, Stage.DEBATERS_PREPARATION);
        });
    }

    private void startDebatersPreparationStage(Guild guild) {
        debatersPreparationStage.play(guild, () -> {
            CompletableFuture<Void> oppositionMove = moveMembersAsync(oppositionDebaters, oppositionVoiceChannel);
            CompletableFuture<Void> governmentMove = moveMembersAsync(governmentDebaters, governmentVoiceChannel);

            oppositionMove.thenCompose(v -> governmentMove).thenRun(() -> {
                startStage(guild, Stage.CONVERSATION_DEBATERS_PREPARATION);
            });
        });
    }


    private void startConversationDebatersPrepareStage(Guild guild) {
        conversationDebatersPrepareStage.play(guild, () -> {
            moveMembersAsync(allDebaters, tribuneVoiceChannel).thenRun(() -> {
                System.out.println("Дебатеры перемещены в трибуну.");
                startStage(guild, Stage.HEAD_GOVERNMENT_FIRST_SPEECH);
            });
        });
    }

    private void startHeadGovernmentFirstSpeechStage(Guild guild) {
        System.out.println("речь главы правительства");
        headGovernmentFirstStage.play(guild, () -> {
            startStage(guild, Stage.HEAD_OPPOSITION_FIRST_SPEECH);
        });
    }

    private void startHeadOppositionFirstSpeechStage(Guild guild) {
        headOppositionFirstStage.play(guild, () -> {
            startStage(guild, Stage.MEMBER_GOVERNMENT_SPEECH);
        });
    }

    private void startMemberGovernmentSpeechStage(Guild guild) {
        memberGovernmentStage.play(guild, () -> {
            startStage(guild, Stage.MEMBER_OPPOSITION_SPEECH);
        });
    }

    private void startMemberOppositionSpeechStage(Guild guild) {
        memberOppositionStage.play(guild, () -> {
            startStage(guild, Stage.HEAD_OPPOSITION_LAST_SPEECH);
        });
    }

    private void startHeadOppositionLastSpeechStage(Guild guild) {
        headOppositionLastStage.play(guild, () -> {
            startStage(guild, Stage.HEAD_GOVERNMENT_LAST_SPEECH);
        });
    }

    private void startHeadGovernmentLastSpeechStage(Guild guild) {
        headGovernmentLastStage.play(guild, () -> {
            startStage(guild, Stage.JUDGES_PREPARATION);
        });
    }

    private void startJudgesPreparationStage(Guild guild) {
        PlayerManager.get().play(guild, AUDIO_RESOURCE_PATH + "Подготовка судей.mp3", (track) -> {
            Utils.startTimer(1, () -> {
                moveMembers(judges, judgesVoiceChannel, null);

                Utils.startTimer(JUDGES_PREPARATION_TIME, () -> {
                    moveMembers(judges, tribuneVoiceChannel, null);
                    startStage(guild, Stage.HEAD_GOVERNMENT_FIRST_SPEECH);
                });

                PlayerManager.get().play(guild, AUDIO_RESOURCE_PATH + "Разговоры пока судьи готовятся.mp3", null);
            });

        });
    }

    public void moveMembers(Set<Member> members, VoiceChannel targetChannel, Runnable callback) {
        for (Member member : members) {
            if (member.getVoiceState() != null && member.getVoiceState().inAudioChannel()) {
                member.getGuild().moveVoiceMember(member, targetChannel).queue(success -> {
                    try {
                        if (callback != null) callback.run();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                System.out.println("Участник " + member.getEffectiveName() + " не находится в голосовом канале.");
            }
        }
    }


    public CompletableFuture<Void> moveMembersAsync(Set<Member> members, VoiceChannel targetChannel) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicInteger counter = new AtomicInteger(members.size());

        if (members.isEmpty()) { // Если никаких участников для перемещения нет, завершаем CompletableFuture сразу
            future.complete(null);
            return future;
        }

        for (Member member : members) {
            if (member.getVoiceState() != null && member.getVoiceState().inAudioChannel()) {
                if (member.getVoiceState().getChannel().equals(targetChannel)) {
                    // Участник уже в целевом канале
                    System.out.println("Участник " + member.getEffectiveName() + " уже в целевом канале.");
                    if (counter.decrementAndGet() == 0) {
                        future.complete(null);
                    }
                } else {
                    member.getGuild().moveVoiceMember(member, targetChannel).queue(success -> {
                        System.out.println("Участник " + member.getEffectiveName() + " успешно перемещен.");
                        if (counter.decrementAndGet() == 0) {
                            future.complete(null);
                        }
                    }, failure -> {
                        System.out.println("Не удалось переместить участника: " + member.getEffectiveName());
                        future.completeExceptionally(failure); // Передаем исключение в CompletableFuture
                    });
                }
            } else {
                System.out.println("Участник " + member.getEffectiveName() + " не находится в голосовом канале.");
                if (counter.decrementAndGet() == 0) {
                    future.complete(null);
                }
            }
        }

        return future;
    }


    @FunctionalInterface
    public interface OnMoveMembersCallback {
        void onMoveMembers();
    }

}
