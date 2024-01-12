package org.example.ui;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.example.resources.StringRes;
import org.example.ui.constants.TextChannelsID;
import org.example.data.repository.ApiRepository;
import org.example.data.repository.DbRepository;
import org.example.ui.constants.VoiceChannelsID;
import org.example.ui.channels.SubscribeTextChannel;
import org.jetbrains.annotations.NotNull;

public class MainListenerAdapter extends ListenerAdapter {
    ApiRepository apiRepository;
    DbRepository dbRepository;
//    RatingTextChannel ratingTextChat;
    SubscribeTextChannel subscribeTextChat;
    StringRes stringsRes;

    public void onReady(@NotNull ReadyEvent event) {
        stringsRes = new StringRes("ru");

        apiRepository = new ApiRepository(event.getJDA());
        dbRepository = new DbRepository();
//        ratingTextChat = new RatingTextChannel(apiRepository, dbRepository);
        subscribeTextChat = new SubscribeTextChannel(apiRepository, dbRepository, stringsRes);
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelLeft() != null) {
            long channelId = event.getChannelLeft().getIdLong();
            String leftChannelName = event.getChannelLeft().getName();

            if (channelId == VoiceChannelsID.WAITING_ROOM) {
                subscribeTextChat.onLeaveFromVoiceChannel(event);
            }

            if (leftChannelName.equals(stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE))) {
                if (subscribeTextChat != null && subscribeTextChat.debateController != null) {
                    subscribeTextChat.debateController.onLeaveFromVoiceChannel(event);
                }
            }
        }

        if (event.getChannelJoined() != null) {
            String joinedChannelName = event.getChannelJoined().getName();

            if (joinedChannelName.equals(stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE))) {
                subscribeTextChat.debateController.onJoinToVoiceChannel(event);
            }

        }
    }


    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getChannel().getType() == ChannelType.TEXT) {
            long channelId = event.getChannel().asTextChannel().getIdLong();

            if (channelId == TextChannelsID.RATING) {
            }

        }
    }

    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String channelName = event.getChannel().getName();
        long channelId = event.getChannel().asTextChannel().getIdLong();

        if (event.getChannelType().equals(ChannelType.TEXT)) {
            if (channelId == TextChannelsID.SUBSCRIBE) {
                subscribeTextChat.onButtonInteraction(event);
            } else if (channelName.equals(stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE))) {
                subscribeTextChat.debateController.onButtonInteraction(event);
            }
        } else {
            if (channelName.equals(stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE))) {
                subscribeTextChat.debateController.onButtonInteraction(event);
            }else if (channelName.equals(stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE))) {
                subscribeTextChat.debateController.onButtonInteraction(event);
            }
        }
    }


}
