package org.example;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import org.example.APIs.NasaAPI;
import org.example.APIs.WeatherAPI;
import org.example.music.*;

import java.io.FileInputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class SlashCommandListener extends ListenerAdapter {
    private final AudioPlayerManager playerManager;
    private final Map<Long, GuildMusicManager> musicManagers = new HashMap<>();

    public SlashCommandListener() {
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
        AudioSourceManagers.registerLocalSource(playerManager);
    }

    private synchronized GuildMusicManager getGuildAudioPlayer(Guild guild, AudioChannel channel) {
        long guildId = guild.getIdLong();
        return musicManagers.computeIfAbsent(guildId, id -> {
            GuildMusicManager manager = new GuildMusicManager(playerManager, guild, channel);
            guild.getAudioManager().setSendingHandler(manager.getSendHandler());
            return manager;
        });
    }

    private boolean isUnpleasantUser(User user) {
        String userId = user.getId();
        String[] unpleasantUsers = {};
        for (String id : unpleasantUsers) {
            if (id.equals(userId)) {
                return true;
            }
        }
        return false;
    }

    public static Properties loadSecrets() {
        Properties secrets = new Properties();
        try (FileInputStream fis = new FileInputStream("secrets.properties")) {
            secrets.load(fis);
        } catch (Exception e) {
            System.err.println("Failed to load secrets.properties: " + e.getMessage());
        }
        return secrets;
    }

    Properties secret = loadSecrets();
    OptionMapping option;
    String apiKey;

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (isUnpleasantUser(event.getUser())) {
            event.getChannel().sendMessage("You are banned from using this bot").queue();
            return;
        }
        switch (event.getName()) {
            case "purge":
                option = event.getOption("count");
                int amount = option.getAsInt();
                if (event.getChannel() instanceof MessageChannel channel) {
                    channel.getHistory().retrievePast(amount).queue(messages -> {
                        channel.purgeMessages(messages);
                        event.reply(amount + " message" + (amount == 1 ? "" : "s") + " ha" + (amount == 1 ? "s" : "ve") + " been deleted").queue();
                    });
                } else {
                    event.reply("error deleting messages").setEphemeral(true).queue();
                }
                break;

            case "vibecheck":
                if (event.getChannel() instanceof MessageChannel channel) {
                    String[] responses = {
                            "✅ nice", "❌ not nice brother", "🤨 you are questionable", "😎 shiny", "🚫 behave",
                            "🧘‍♂️ go take a chill", "🥴 i don't think you qualify",
                            "🌈 amazing", "☠️ no that's not how we do things here", "📉 you better make up for it because it's rapidly going down"
                    };
                    int randomIndex = (int) (Math.random() * responses.length);
                    String vibeResult = responses[randomIndex];
                    event.reply(vibeResult).queue();
                    if (randomIndex == 4 && event.isFromGuild() && event.getMember() != null) {
                        try {
                            event.getMember().timeoutFor(java.time.Duration.ofMinutes(1))
                                    .queue(
                                            success -> channel.sendMessage("nice you have won the lottery and now get to have a timeout for 1 minute").queue(),
                                            error -> channel.sendMessage("unfortunately i can't timeout you, but consider yourself a winner of a 1 minute timeout").queue()
                                    );
                        } catch (Exception e) {
                            channel.sendMessage("can't timeout you. unfortunate.").queue();
                        }
                    }
                }
                break;

            case "playsound":
                option = event.getOption("sound");
                String sound = option.getAsString();
                var member = event.getMember();
                var guild = event.getGuild();
                var vc = member.getVoiceState().getChannel();
                if (vc == null) {
                    event.reply("join a voice channel for me real quick").queue();
                    return;
                }
                var audioManager = guild.getAudioManager();
                AudioPlayer player = playerManager.createPlayer();
                audioManager.setSendingHandler(new LavaAudioPlayerHandler(player));
                audioManager.openAudioConnection(vc);
                event.reply("one sec").queue();
                URL resource = getClass().getClassLoader().getResource("sounds/" + sound + ".mp3");
                String path = "";
                if (resource != null) {
                    path = resource.getPath();
                    path = URLDecoder.decode(path, StandardCharsets.UTF_8);
                } else {
                    System.out.println("sound file not found");
                }
                System.out.println(path);
                playerManager.loadItem(path, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        player.playTrack(track);
                        new Thread(() -> {
                            try {
                                Thread.sleep(track.getDuration());
                                audioManager.closeAudioConnection();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                    @Override public void playlistLoaded(AudioPlaylist playlist) {}
                    @Override public void noMatches() {}
                    @Override public void loadFailed(FriendlyException ex) {
                        ex.printStackTrace();
                    }
                });
                break;

            case "weather":
                option = event.getOption("city");
                String city = option.getAsString();
                String apiKeyWea = secret.getProperty("weather.api");
                String apiKeyDeep = secret.getProperty("deepseek.api");
                event.deferReply().queue(hook -> {
                    CompletableFuture
                            .supplyAsync(() -> WeatherAPI.getWeatherInfo(city, apiKeyDeep, apiKeyWea))
                            .thenAccept(weather -> {
                                hook.sendMessage(weather != null ? weather : "could not retrieve the weather, maybe you made a typo?").queue();
                            });
                });
                break;

            case "nasa":
                String function = event.getOption("function").getAsString();
                String date = event.getOption("date") != null ? event.getOption("date").getAsString() : null;
                event.deferReply().queue();
                apiKey = secret.getProperty("nasa.api");
                new Thread(() -> {
                    EmbedBuilder embed = NasaAPI.getNasaInfo(function, date, apiKey);
                    event.getHook().sendMessageEmbeds(embed != null ? embed.build() : null).queue(
                            null,
                            throwable -> event.getHook().sendMessage("Error creating an embed").queue()
                    );
                }).start();
                break;

            case "play":
                String url = event.getOption("url").getAsString();
                member = event.getMember();
                guild = event.getGuild();
                vc = member != null ? member.getVoiceState().getChannel() : null;
                if (vc == null) {
                    event.reply("join a voice channel first").setEphemeral(true).queue();
                    return;
                }
                audioManager = guild.getAudioManager();
                GuildMusicManager musicManager = PlayerManager.getInstance().getGuildMusicManager(guild, vc);
                TrackScheduler scheduler = musicManager.scheduler;
                player = musicManager.player;
                player.addListener(scheduler);
                audioManager.setSendingHandler(new LavaAudioPlayerHandler(player));
                audioManager.openAudioConnection(vc);
                event.reply("searching and streaming: " + url).queue();
                TrackScheduler finalScheduler = scheduler;
                playerManager.loadItem(url, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        finalScheduler.queue(track);
                        try {
                            PlaybackState state = new PlaybackState();
                            state.guildId = guild.getId();
                            state.channelId = vc.getId();
                            state.trackUrl = track.getInfo().uri;
                            state.position = 0;
                            PlaybackStateManager.saveState(guild.getId(), state);
                        } catch (Exception e) {
                            System.err.println("failed to save playback state for guild " + guild.getId() + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    @Override public void playlistLoaded(AudioPlaylist playlist) {
                        event.getHook().sendMessage("playlist added: " + playlist.getName()).queue();
                        for (AudioTrack track : playlist.getTracks()) {
                            finalScheduler.queue(track);
                        }
                        if (!playlist.getTracks().isEmpty()) {
                            try {
                                AudioTrack firstTrack = playlist.getTracks().get(0);
                                PlaybackState state = new PlaybackState();
                                state.guildId = guild.getId();
                                state.channelId = vc.getId();
                                state.trackUrl = firstTrack.getInfo().uri;
                                state.position = 0;
                                PlaybackStateManager.saveState(guild.getId(), state);
                            } catch (Exception e) {
                                System.err.println("failed to save playback state for guild " + guild.getId() + ": " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                    @Override public void noMatches() {
                        event.getHook().sendMessage("nothing was found with that URL").queue();
                    }
                    @Override public void loadFailed(FriendlyException e) {
                        event.getHook().sendMessage("error loading track: " + e.getMessage()).queue();
                        e.printStackTrace();
                    }
                });
                break;

            case "stop":
                musicManager = PlayerManager.getInstance().getGuildMusicManager(event.getGuild(), null);
                musicManager.scheduler.getQueue().clear();
                musicManager.player.stopTrack();
                event.getGuild().getAudioManager().closeAudioConnection();
                PlaybackStateManager.clearState(event.getGuild().getId()); // Clear guild-specific state
                event.reply("stopped and cleared the queue").queue();
                break;

            case "skip":
                musicManager = getGuildAudioPlayer(event.getGuild(), null);
                scheduler = musicManager.scheduler;
                AudioTrack currentTrack = musicManager.player.getPlayingTrack();
                if (currentTrack == null) {
                    event.reply("nothing is playing").setEphemeral(true).queue();
                    return;
                }
                event.reply("skipped **" + currentTrack.getInfo().title + "**").queue(success -> {
                    scheduler.nextTrack();
                }, failure -> {
                    System.err.println("failed to reply to skip command: " + failure.getMessage());
                });
                break;

            case "queue":
                scheduler = getGuildAudioPlayer(event.getGuild(), null).scheduler;
                if (scheduler.getQueue().isEmpty()) {
                    event.reply("the queue is empty, want to play some music?").queue();
                } else {
                    StringBuilder builder = new StringBuilder("queue (first 20):\n");
                    int i = 1;
                    for (AudioTrack trackCurrent : scheduler.getQueue().stream().limit(20).toList()) {
                        builder.append(i++).append(". ").append(trackCurrent.getInfo().title).append("\n");
                    }
                    event.reply(builder.toString()).queue();
                }
                break;
        }
    }
}