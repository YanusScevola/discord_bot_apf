package org.example.core;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.example.data.source.ApiService;
import org.example.data.source.db.Database;
import org.example.resources.StringRes;
import org.example.core.constants.TextChannelsID;
import org.example.domain.UseCase;
import org.example.core.constants.VoiceChannelsID;
import org.example.core.controllers.RatingController;
import org.example.core.controllers.SubscribeController;
import org.jetbrains.annotations.NotNull;

public class MainListenerAdapter extends ListenerAdapter {

    RatingController ratingTextChat;
    SubscribeController subscribeTextChat;
    StringRes stringsRes;

    public void onReady(@NotNull ReadyEvent event) {
        stringsRes = new StringRes("ru");

        ApiService apiService = ApiService.getInstance(event.getJDA());
        Database database = Database.getInstance();
        UseCase useCase = UseCase.getInstance(apiService, database);

        ratingTextChat = new RatingController(useCase);
        subscribeTextChat = new SubscribeController(useCase, stringsRes);
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        AudioChannel channelJoined = event.getChannelJoined();
        AudioChannel channelLeft = event.getChannelLeft();

//        boolean isJoinToDebateChannel = isDebateChannel(channelJoined);
//        boolean isLeaveFromDebateChannel = isDebateChannel(channelLeft);


        String channelJoinedName = channelJoined != null ? channelJoined.getName() : "";
        String channelLeftName = channelLeft != null ? channelLeft.getName() : "";
        long channelJoinedId = channelJoined != null ? channelJoined.getIdLong() : -1;
        long channelLeftId = channelLeft != null ? channelLeft.getIdLong() : -1;

        if (channelJoinedName.equals(stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE))) {
            if (subscribeTextChat.debateController != null) {
                subscribeTextChat.debateController.onJoinToTribuneVoiceChannel(event.getGuild(), channelJoined, event.getMember());
            }
        }

        if (channelLeftId == VoiceChannelsID.WAITING_ROOM) {
            subscribeTextChat.onLeaveFromVoiceChannel(event);
        }else if (channelLeftName.equals(stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE))) {
            if (subscribeTextChat.debateController != null) {
                subscribeTextChat.debateController.onLeaveFromTribuneVoiceChannel(event.getGuild(), channelJoined, event.getMember());
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
        if (event.getChannelType().equals(ChannelType.TEXT)) {
            long channelId = event.getChannel().asTextChannel().getIdLong();
            if (channelId == TextChannelsID.SUBSCRIBE) {
                subscribeTextChat.onButtonInteraction(event);
            } else if (channelName.equals(stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE))) {
                subscribeTextChat.debateController.onButtonInteraction(event);
            }
        } else {
            if (channelName.equals(stringsRes.get(StringRes.Key.CHANNEL_TRIBUNE))) {
                subscribeTextChat.debateController.onButtonInteraction(event);
            } else if (channelName.equals(stringsRes.get(StringRes.Key.CHANNEL_JUDGE))) {
                subscribeTextChat.debateController.onButtonInteraction(event);
            }
        }
    }

}
