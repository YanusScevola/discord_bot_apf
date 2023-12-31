package org.example.ui.channels;

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.resources.StringRes;
import org.jetbrains.annotations.NotNull;

public class JudgeVoiceChannel {
    VoiceChannel channel;
    ApiRepository apiRepository;
    DbRepository dbRepository;
    StringRes stringsRes;


    public JudgeVoiceChannel(ApiRepository apiRepository, DbRepository dbRepository, VoiceChannel channel) {
        this.apiRepository = apiRepository;
        this.dbRepository = dbRepository;
        this.stringsRes = StringRes.getInstance(StringRes.Language.RUSSIAN);

        if (this.channel == null) this.channel = channel;

        channel.getHistoryFromBeginning(1).queue(history -> {

        });
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {

    }

    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent event) {
        System.out.println(event.getMember().getEffectiveName() + " moved to " + event.getChannelJoined().getName());
    }
}
