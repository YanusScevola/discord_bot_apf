package org.example.ui;

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;

public interface Chat {
    public void onGuildMemberJoin(GuildMemberJoinEvent event);
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event);
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event);
    public void onMessageReceived(MessageReceivedEvent event);
    public void onButtonInteraction(ButtonInteractionEvent event);
}
