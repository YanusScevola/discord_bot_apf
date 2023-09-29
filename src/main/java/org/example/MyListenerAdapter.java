package org.example;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.example.constants.ChannelIds;
import org.example.repository.ApiRepository;
import org.example.repository.DbRepository;
import org.example.channels.MenuTextChannel;
import org.jetbrains.annotations.NotNull;

public class MyListenerAdapter extends ListenerAdapter {
    ApiRepository apiRepository;
    DbRepository dbRepository;
    MenuTextChannel ratingTextChat;


    public void onReady(@NotNull ReadyEvent event) {
        apiRepository = new ApiRepository(event.getJDA());
        dbRepository = new DbRepository();

        ratingTextChat = new MenuTextChannel(apiRepository, dbRepository);

    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        ratingTextChat.onGuildMemberJoin(event);
    }

    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        ratingTextChat.onGuildMemberRoleAdd(event);
    }

    @Override
    public void onGuildMemberRoleRemove(@NotNull GuildMemberRoleRemoveEvent event) {
        ratingTextChat.onGuildMemberRoleRemove(event);
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getChannel().getType() == ChannelType.TEXT) {
            String channelId = String.valueOf(event.getChannel().asTextChannel().getIdLong());

            if (channelId.equals(ChannelIds.RATING)) {
                ratingTextChat.onMessageReceived(event);
            }

        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        String channelId = String.valueOf(event.getChannel().asTextChannel().getIdLong());

        if (channelId.equals(ChannelIds.RATING)) {
        }

    }


    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String channelId = String.valueOf(event.getChannel().asTextChannel().getIdLong());

        if (channelId.equals(ChannelIds.RATING)) {
            ratingTextChat.onStringSelectInteraction(event);
        }
    }





}
