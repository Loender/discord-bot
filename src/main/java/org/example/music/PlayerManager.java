package org.example.music;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerManager {
    private static final PlayerManager INSTANCE = new PlayerManager();
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers;

    private PlayerManager() {
        this.playerManager = new DefaultAudioPlayerManager();
        this.musicManagers = new ConcurrentHashMap<>();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    public static PlayerManager getInstance() {
        return INSTANCE;
    }

    public GuildMusicManager getGuildMusicManager(Guild guild) {
        return this.musicManagers.computeIfAbsent(guild.getIdLong(),
                (guildId) -> new GuildMusicManager(playerManager));
    }

    public AudioPlayerManager getPlayerManager() {
        return playerManager;
    }
}
