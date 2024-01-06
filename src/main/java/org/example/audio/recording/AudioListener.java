package org.example.audio.recording;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import javax.sound.sampled.*;
import java.io.*;

public class AudioListener implements AudioReceiveHandler {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private AudioFormat format = new AudioFormat(48000.0f, 16, 2, true, true);

    @Override
    public boolean canReceiveCombined() {
        // Этот метод должен возвращать true, чтобы указать,
        // что мы хотим получать комбинированные аудиоданные всех пользователей.
        return true;
    }

    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        // Этот метод будет вызываться JDA каждый раз, когда есть комбинированные аудиоданные для обработки.
        byte[] data = combinedAudio.getAudioData(1.0); // Получение данных
        try {
            outputStream.write(data); // Запись данных в поток
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveAudio() {
        // Этот метод используется для сохранения накопленных аудиоданных в файл.
        try {
            byte[] audioData = outputStream.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, new File("audio.wav")); // Запись файла
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canReceiveUser() {
        // Мы не хотим получать аудиоданные отдельных пользователей.
        return false;
    }

    @Override
    public void handleUserAudio(net.dv8tion.jda.api.audio.UserAudio userAudio) {
        // Метод для обработки аудио отдельного пользователя, нам это не нужно.
    }
}
