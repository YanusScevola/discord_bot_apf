package org.example.interfaces;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface MessageEventListener {
    public void onMessageReceived(MessageReceivedEvent event);
}
