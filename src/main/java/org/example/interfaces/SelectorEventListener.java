package org.example.interfaces;

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public interface SelectorEventListener {
    public void onStringSelectInteraction(StringSelectInteractionEvent event);
}
