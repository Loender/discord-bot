package org.example.music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

public class GuildMusicManager {
    public final AudioPlayer player;
    public final TrackScheduler scheduler;

    public GuildMusicManager(AudioPlayerManager manager, Guild guild, AudioChannel channel) {
        this.player = manager.createPlayer();
        this.scheduler = new TrackScheduler(player, guild, channel); // Pass guild and channel
        this.player.addListener(this.scheduler);
    }

    public LavaAudioPlayerHandler getSendHandler() {
        return new LavaAudioPlayerHandler(player);
    }
}