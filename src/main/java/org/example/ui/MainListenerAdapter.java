package org.example.ui;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.example.resources.StringRes;
import org.example.ui.channels.TribuneVoiceChannel;
import org.example.ui.constants.TextChannelsID;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.ui.constants.VoiceChannelsID;
import org.example.ui.channels.RatingTextChannel;
import org.example.ui.channels.SubscribeTextChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MainListenerAdapter extends ListenerAdapter {
    ApiRepository apiRepository;
    DbRepository dbRepository;
    RatingTextChannel ratingTextChat;
    SubscribeTextChannel subscribeTextChat;
    StringRes stringsRes;

    public void onReady(@NotNull ReadyEvent event) {
        stringsRes = StringRes.getInstance(StringRes.Language.RUSSIAN);
        apiRepository = new ApiRepository(event.getJDA());
        dbRepository = new DbRepository();
        ratingTextChat = new RatingTextChannel(apiRepository, dbRepository);
        subscribeTextChat = new SubscribeTextChannel(apiRepository, dbRepository, stringsRes);
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelLeft() != null) {
            String channelId = String.valueOf(event.getChannelLeft().getIdLong());

            if (channelId.equals(VoiceChannelsID.WAITING_ROOM)) {
                subscribeTextChat.onGuildVoiceUpdate(event);
            }
        }

        if (event.getChannelJoined() != null) {
            String channelName = event.getChannelJoined().getName();

            if (channelName.equals(stringsRes.get(StringRes.Key.TRIBUNE_CHANNEL_NAME))) {
                subscribeTextChat.tribuneVoiceChannel.onGuildVoiceUpdate(event);
            }
        }
    }


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getChannel().getType() == ChannelType.TEXT) {
            String channelId = String.valueOf(event.getChannel().asTextChannel().getIdLong());

            if (channelId.equals(TextChannelsID.RATING)) {
            }

        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String channelId = String.valueOf(event.getChannel().asTextChannel().getIdLong());
        String channelName = event.getChannel().getName();

        if (channelId.equals(TextChannelsID.SUBSCRIBE)) {
            subscribeTextChat.onButtonInteraction(event);
        } else if (channelName.equals(stringsRes.get(StringRes.Key.TRIBUNE_CHANNEL_NAME))) {
            subscribeTextChat.tribuneVoiceChannel.onButtonInteraction(event);
        }


    }


}
