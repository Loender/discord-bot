package org.example.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.*;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.entities.Guild;

import java.util.LinkedList;
import java.util.Queue;

public class TrackScheduler extends AudioEventAdapter {
    private final AudioPlayer player;
    private final Queue<AudioTrack> queue;
    private final Guild guild;
    private final AudioChannel channel;

    public TrackScheduler(AudioPlayer player, Guild guild, AudioChannel channel) {
        this.player = player;
        this.guild = guild;
        this.channel = channel;
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
            player.stopTrack();
            if (channel != null) {
                PlaybackStateManager.clearState(guild.getId());
            }
        }
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            AudioTrack nextTrack = queue.poll();
            if (nextTrack != null) {
                player.startTrack(nextTrack, false);
            } else {
                player.stopTrack();
                if (channel != null) {
                    PlaybackStateManager.clearState(guild.getId());
                }
            }
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        if (track != null && channel != null) {
            PlaybackState state = new PlaybackState();
            state.guildId = guild.getId();
            state.trackUrl = track.getInfo().uri;
            state.channelId = channel.getId();
            state.position = track.getPosition();
            PlaybackStateManager.saveState(guild.getId(), state);
        }
    }

}