package org.example.ui;

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.example.constants.ChannelIds;
import org.example.repository.ApiRepository;
import org.example.repository.DbRepository;
import org.jetbrains.annotations.NotNull;

public class MyListenerAdapter extends ListenerAdapter {
    ApiRepository apiRepository;
    DbRepository dbRepository;
    InfoTextChat infoTextChat;


    public void onReady(ReadyEvent event) {
        apiRepository = new ApiRepository(event.getJDA());
        dbRepository = new DbRepository();

        infoTextChat = new InfoTextChat(apiRepository, dbRepository);

    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        infoTextChat.onGuildMemberJoin(event);
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        infoTextChat.onGuildMemberRoleAdd(event);
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        infoTextChat.onGuildMemberRoleRemove(event);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String channelId = String.valueOf(event.getChannel().asTextChannel().getIdLong());

        if (channelId.equals(ChannelIds.INFORMATION)) {
            infoTextChat.onMessageReceived(event);
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String channelId = String.valueOf(event.getChannel().asTextChannel().getIdLong());

        if (channelId.equals(ChannelIds.INFORMATION)) {
            infoTextChat.onButtonInteraction(event);
        }

    }


}
