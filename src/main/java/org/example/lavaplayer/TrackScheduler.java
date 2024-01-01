package org.example.lavaplayer;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TrackScheduler extends AudioEventAdapter {

    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue = new LinkedBlockingQueue<>();
    private boolean isRepeat = false;
    private  TrackEndCallback callback;
    private AudioTrack currentTrack; // Текущий трек
    private TrackEndCallback endCallback; // Колбек для текущего трека


    public TrackScheduler(AudioPlayer player) {
        this.player = player;
    }



    public void setEndCallback(AudioTrack track, TrackEndCallback endCallback) {
        this.currentTrack = track;
        this.endCallback = endCallback;
    }

    public void onTrackEnd(AudioTrack track) {
        if (track.equals(currentTrack) && endCallback != null) {
            endCallback.onTrackEnd(track);
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (track.equals(currentTrack) && endCallback != null) {
            endCallback.onTrackEnd(track);
        }

        if (isRepeat) {
            player.startTrack(track.makeClone(), false);
        } else {
            AudioTrack nextTrack = queue.poll();
            if (nextTrack != null) {
                setEndCallback(nextTrack, endCallback); // Переустанавливайте колбек для следующего трека
                player.startTrack(nextTrack, false);
            }
        }
    }
    public void queue(AudioTrack track) {
        if(!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    @FunctionalInterface
    public interface TrackEndCallback {
        void onTrackEnd(AudioTrack track);
    }
    public AudioPlayer getPlayer() {
        return player;
    }

    public BlockingQueue<AudioTrack> getQueue() {
        return queue;
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    public void setRepeat(boolean repeat) {
        isRepeat = repeat;
    }
}
