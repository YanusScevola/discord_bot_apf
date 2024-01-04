package org.example.ui.interfaces;

import net.dv8tion.jda.api.entities.Member;

@FunctionalInterface
public interface MuteSuccessCallback {
    void onSuccess();
}
