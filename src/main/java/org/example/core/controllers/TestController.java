package org.example.core.controllers;

import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.example.core.constants.TextChannelsID;
import org.example.domain.UseCase;

public class TestController {
    private static TestController instance;
    private TextChannel channel;
    private UseCase useCase;

    private TestController(UseCase useCas, TextChannel channel) {
        this.useCase = useCase;
        this.channel = channel;
    }

    public static synchronized TestController getInstance(UseCase useCase, TextChannel channel) {
        if (instance == null) {
            instance = new TestController(useCase, channel);
        }
        return instance;
    }

    public void startTest() {

    }

}
