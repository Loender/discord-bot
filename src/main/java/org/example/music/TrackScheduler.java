package org.example.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.*;


import java.util.LinkedList;
import java.util.Queue;

public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final Queue<AudioTrack> queue;

    public TrackScheduler(AudioPlayer player) {
        this.player = player;
        this.queue = new LinkedList<>();
    }

    public void queue(AudioTrack track) {
        if (!player.startTrack(track, true)) {
            queue.offer(track);
        }
    }

    public Queue<AudioTrack> getQueue() {
        return queue;
    }

    public void nextTrack() {
        AudioTrack next = queue.poll();
        if (next != null) {
            player.playTrack(next);
        } else {
            player.stopTrack(); // no more tracks
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            player.startTrack(queue.poll(), false);
        }
    }
}
