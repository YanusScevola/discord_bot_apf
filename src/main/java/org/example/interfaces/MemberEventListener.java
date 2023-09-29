package org.example.interfaces;

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

public interface MemberEventListener {
    public void onGuildMemberJoin(GuildMemberJoinEvent event);
}
