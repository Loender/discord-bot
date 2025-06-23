package org.example;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.example.music.*;
import org.example.APIs.*;
import org.example.music.PlaybackState;

import javax.security.auth.login.LoginException;
import java.io.FileInputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Map;

public class Bot {

    public static void main(String[] args) throws LoginException, InterruptedException {
        String token = loadSecrets().getProperty("bot.token");
        JDA jda = JDABuilder.createDefault(token)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(new MessageListener())
                .addEventListeners(new SlashCommandListener())
                .build();
        jda.awaitReady();
        jda.updateCommands().addCommands(
                Commands.slash("vibecheck", "check your vibe for today"),
                Commands.slash("purge", "delete messages")
                        .addOption(OptionType.INTEGER, "count", "how many messages to purge", true),
                Commands.slash("playsound", "make a sound")
                        .addOption(OptionType.STRING, "sound", "sound1, sound2 ...", true),
                Commands.slash("weather", "show weather in a city")
                        .addOption(OptionType.STRING, "city", "enter city", true),
                Commands.slash("nasa", "nasa functions")
                        .addOption(OptionType.STRING, "function", "available functions: apod", true)
                        .addOption(OptionType.STRING, "date", "enter date", false),
                Commands.slash("play", "play some tunes!")
                        .addOption(OptionType.STRING, "url", "soundcloud link", true),
                Commands.slash("queue", "show queue"),
                Commands.slash("skip", "skip current tracks"),
                Commands.slash("stop", "stop the music")
        ).queue();

        Map<String, PlaybackState> stateMap = PlaybackStateManager.loadStateMap();
        for (String guildId : stateMap.keySet()) {
            PlaybackState state = PlaybackStateManager.loadState(guildId);
            if (state != null) {
                System.out.println("Resuming music for guild: " + state.guildId);
                Guild guild = jda.getGuildById(state.guildId);
                if (guild == null) {
                    System.out.println("Guild not found: " + state.guildId);
                    PlaybackStateManager.clearState(state.guildId);
                }
                AudioManager audioManager = guild.getAudioManager();
                VoiceChannel vc = guild.getVoiceChannelById(state.channelId);
                if (vc == null) {
                    System.out.println("Voice channel not found: " + state.channelId);
                    PlaybackStateManager.clearState(state.guildId);
                }
                audioManager.openAudioConnection(vc);
                AudioPlayerManager playerManager = PlayerManager.getInstance().getPlayerManager();
                GuildMusicManager musicManager = PlayerManager.getInstance().getGuildMusicManager(guild, vc);
                playerManager.loadItem(state.trackUrl, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack track) {
                        track.setPosition(state.position);
                        musicManager.player.playTrack(track);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist playlist) {
                    }

                    @Override
                    public void noMatches() {
                        System.out.println("No matches found for track URL: " + state.trackUrl);
                        PlaybackStateManager.clearState(state.guildId);
                    }

                    @Override
                    public void loadFailed(FriendlyException e) {
                        System.out.println("Failed to load track on resume for guild " + state.guildId + ": " + e.getMessage());
                        PlaybackStateManager.clearState(state.guildId);
                    }
                });
            }
        }
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


    public static class MessageListener extends ListenerAdapter {
        String apiKey;
        private final AudioPlayerManager playerManager;
        Properties secret = loadSecrets();

        public MessageListener() {
            playerManager = new DefaultAudioPlayerManager();
            AudioSourceManagers.registerRemoteSources(playerManager);
            AudioSourceManagers.registerLocalSource(playerManager);
        }

        private boolean isUnpleasantUser(MessageReceivedEvent event) {
            String userId = event.getAuthor().getId();

            String[] unpleasantUsers = {
            };

            for (String id : unpleasantUsers) {
                if (id.equals(userId)) {
                    return true;
                }
            }
            return false;
        }


        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            Message message = event.getMessage();
            String content = message.getContentRaw();
            MessageChannel channel = event.getChannel();


            if (isUnpleasantUser(event)) {
                event.getChannel().sendMessage("You have been banned from using this bot").queue();
                return;
            }


            if (content.contains(event.getJDA().getSelfUser().getAsMention())) {
                String prompt = content.replace(event.getJDA().getSelfUser().getAsMention(), "").trim();

                if (!prompt.isEmpty()) {
                    channel.sendTyping().queue();
                    AgentAPI.AgenticAiResponse reply = AgentAPI.queryDeepseek(prompt);

                    if (reply != null) {
                        switch (reply.intent.toLowerCase()) {
                            case "vibe" -> {
                                String[] responses = {
                                        "✅ nice",
                                        "❌ not nice brother",
                                        "🤨 you are questionable",
                                        "😎 shiny",
                                        "🚫 behave",
                                        "🧘‍♂️ go take a chill",
                                        "🥴 i don't think you qualify",
                                        "🌈 amazing",
                                        "☠️ no that's not how we do things here",
                                        "📉 you better make up for it because it's rapidly going down"
                                };

                                int randomIndex = (int) (Math.random() * responses.length);
                                String vibeResult = responses[randomIndex];

                                channel.sendMessage(vibeResult).queue();
                                if (randomIndex == 4 && event.isFromGuild() && event.getMember() != null) {
                                    try {
                                        event.getMember().timeoutFor(java.time.Duration.ofMinutes(1))
                                                .queue(
                                                        success -> channel.sendMessage("nice you have won the lottery and now get to have a timeout for 1 minute").queue(),
                                                        error -> channel.sendMessage("unfortunately i can't timeout you, but consider" +
                                                                " yourself a winner of a 1 minute timeout").queue()
                                                );
                                    } catch (Exception e) {
                                        channel.sendMessage("can't timeout you. unfortunate.").queue();
                                    }
                                }
                            }
                            case "sound" -> {
                                var member = event.getMember();
                                var guild = event.getGuild();
                                var vc = member.getVoiceState().getChannel();

                                if (vc == null) {
                                    message.reply("join a voice channel for me real quick").queue();
                                    return;
                                }

                                var audioManager = guild.getAudioManager();
                                AudioPlayer player = playerManager.createPlayer();
                                audioManager.setSendingHandler(new LavaAudioPlayerHandler(player));
                                audioManager.openAudioConnection(vc);

                                message.reply("one sec").queue();
                                int rand = (int) (Math.random() * 4);
                                String path = "";
                                URL resource = getClass().getClassLoader().getResource("sounds/sound" + rand + ".mp3");
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
                                                Thread.sleep(track.getDuration() + 750);
                                                audioManager.closeAudioConnection();
                                            } catch (InterruptedException e) {
                                                e.printStackTrace();
                                            }
                                        }).start();
                                    }

                                    @Override
                                    public void playlistLoaded(com.sedmelluq.discord.lavaplayer.track.AudioPlaylist playlist) {
                                    }

                                    @Override
                                    public void noMatches() {
                                    }

                                    @Override
                                    public void loadFailed(com.sedmelluq.discord.lavaplayer.tools.FriendlyException ex) {
                                    }
                                });
                            }
                            case "cat" -> {
                                apiKey = secret.getProperty("cat.api");
                                CompletableFuture.runAsync(() -> {
                                    String imageUrl = CatAPI.getRandomCatImageUrl(apiKey);

                                    if (imageUrl != null) {
                                        message.reply(imageUrl).queue();
                                    } else {
                                        message.reply("no cat today 😿").queue();
                                    }
                                });
                            }
                            case "music" -> {
                                String url = reply.url;
                                var member = event.getMember();
                                var guild = event.getGuild();
                                var vc = member != null ? member.getVoiceState().getChannel() : null;
                                if (vc == null) {
                                    message.reply("join a voice channel first").queue();
                                    return;
                                }
                                var audioManager = guild.getAudioManager();
                                GuildMusicManager musicManager = PlayerManager.getInstance().getGuildMusicManager(guild, vc);
                                TrackScheduler scheduler = musicManager.scheduler;
                                AudioPlayer player = musicManager.player;
                                player.addListener(scheduler);
                                audioManager.setSendingHandler(new LavaAudioPlayerHandler(player));
                                audioManager.openAudioConnection(vc);
                                message.reply("searching and streaming: " + url).queue();
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

                                    @Override
                                    public void playlistLoaded(AudioPlaylist playlist) {
                                        message.reply("playlist added: " + playlist.getName()).queue();
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

                                    @Override
                                    public void noMatches() {
                                        message.reply("nothing was found with that URL").queue();
                                    }

                                    @Override
                                    public void loadFailed(FriendlyException e) {
                                        message.reply("error loading track: " + e.getMessage()).queue();
                                        e.printStackTrace();
                                    }
                                });
                            }
                            case "nasa" -> {
                                String function = "apod";
                                apiKey = secret.getProperty("nasa.api");
                                channel.sendTyping().queue();

                                new Thread(() -> {
                                    EmbedBuilder embed = NasaAPI.getNasaInfo(function, null, apiKey);
                                    if (embed != null) {
                                        channel.sendMessageEmbeds(embed.build()).queue();
                                    } else {
                                        channel.sendMessage("something went wrong retrieving NASA data").queue();
                                    }
                                }).start();
                            }
                            default -> channel.sendMessage(reply.response).queue();
                        }

                    } else {
                        message.reply("Unexpected error").queue();
                    }
                } else {
                    message.reply("hey!").queue();
                }
                return;
            }

            if (message.getAuthor().isBot()) return;
            if (content.contains("league of legends")) {
                if (event.isFromGuild()) {
                    try {
                        event.getGuild().ban(event.getMember(), 0, TimeUnit.SECONDS)
                                .reason("go think about how you behave")
                                .queue();
                    } catch (Exception e) {
                        message.reply("if only you were not higher than me you would be banned I swear").queue();
                    }
                } else {
                    channel.sendMessage("don't use that word pls").queue();
                }
            }


            if (content.equalsIgnoreCase("feeling lonely")) {
                var member = event.getMember();
                var guild = event.getGuild();
                var vc = member.getVoiceState().getChannel();

                if (vc == null) {
                    message.reply("join a channel, i will share the silence with you").queue();
                    return;
                }
                message.reply("on my way").queue();
                var audioManager = guild.getAudioManager();
                audioManager.openAudioConnection(vc);
            }
            if (content.equalsIgnoreCase("go away")) {
                var guild = event.getGuild();
                var audioManager = guild.getAudioManager();
                if (!audioManager.isConnected()) {
                    message.reply("what").queue();
                    return;
                }
                message.reply(":sob:").queue();
                audioManager.closeAudioConnection();
            }
            if (content.equalsIgnoreCase("show me your picture")) {
                message.reply("<:lev:1381705639599931624>").queue();
            }
        }
    }
}

