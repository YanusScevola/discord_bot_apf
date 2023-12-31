package org.example.ui.channels;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.lavaplayer.PlayerManager;
import org.example.resources.StringRes;
import org.example.ui.constants.RolesID;
import org.jetbrains.annotations.NotNull;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TribuneVoiceChannel {
    VoiceChannel channel;
    ApiRepository apiRepository;
    DbRepository dbRepository;
    StringRes stringsRes;
    Set<Member> debaters = new HashSet<>();
    Set<Member> judges = new HashSet<>();

    private static final int DEBATERS_LIMIT = 1;

    public TribuneVoiceChannel(ApiRepository apiRepository, DbRepository dbRepository, VoiceChannel channel) {
        this.channel = channel;
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        this.stringsRes = StringRes.getInstance(StringRes.Language.RUSSIAN);
        channel.getHistoryFromBeginning(1).queue(history -> {

        });
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

    }

    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        List<String> roleDebaterIds = List.of(RolesID.HEAD_GOVERNMENT, RolesID.HEAD_OPPOSITION, RolesID.MEMBER_GOVERNMENT, RolesID.MEMBER_OPPOSITION);
        List<String> roleJudgeIds = List.of(RolesID.JUDGE);

        event.getMember().getRoles().forEach(role -> {
            if (roleDebaterIds.contains(role.getId())) debaters.add(event.getMember());
            if (roleJudgeIds.contains(role.getId())) judges.add(event.getMember());
        });
        if (debaters.size() == DEBATERS_LIMIT && judges.size() >= 1) {
            startDebate(event);
        }

    }

    private void startDebate(GuildVoiceUpdateEvent event) {
        PlayerManager.get().play(event.getMember().getGuild(), "src/main/resources/audio/Запись 0.mp3");
    }


}
