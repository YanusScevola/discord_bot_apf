package org.example.interfaces;

import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;

public interface RoleEventListener {
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event);
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event);
}
