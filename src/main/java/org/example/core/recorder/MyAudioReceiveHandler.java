package org.example.core.recorder;

import net.dv8tion.jda.api.audio.AudioReceiveHandler;
import net.dv8tion.jda.api.audio.CombinedAudio;
import net.dv8tion.jda.api.audio.UserAudio;
import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class MyAudioReceiveHandler implements AudioReceiveHandler {
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private AudioFormat format = new AudioFormat(48000.0f, 16, 2, true, true);
    private boolean isRecording = false;

    @Override
    public boolean canReceiveCombined() {
        return isRecording;
    }

    @Override
    public void handleCombinedAudio(CombinedAudio combinedAudio) {
        if (isRecording) {
            byte[] data = combinedAudio.getAudioData(1.0); // Получаем аудиоданные
            try {
                outputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean canReceiveUser() {
        return false;
    }

    @Override
    public void handleUserAudio(UserAudio userAudio) {
        // Не используется
    }

    public void startRecording() {
        isRecording = true;
    }

    public void stopRecording(String filePath) throws IOException {
        isRecording = false;
        outputStream.flush(); // Убедимся, что все данные записаны в поток
        byte[] audioData = outputStream.toByteArray();
        if (audioData.length == 0) {
            System.out.println("В потоке нет данных для записи.");
            return;
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
        AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, format, audioData.length / format.getFrameSize());
        File file = new File(filePath);
        // Проверяем и создаем директории, если необходимо
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, file);
        System.out.println("Запись сохранена в " + filePath);
        outputStream.reset(); // Очищаем поток для следующей записи

    }
}