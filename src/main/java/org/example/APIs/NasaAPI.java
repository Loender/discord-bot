package org.example.APIs;

import net.dv8tion.jda.api.EmbedBuilder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.*;

import java.awt.*;
import java.time.Duration;

public class NasaAPI {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(15)) // total time for the call
            .connectTimeout(Duration.ofSeconds(10)) // time to connect to the server
            .readTimeout(Duration.ofSeconds(15)) // time to read the server response
            .build();

    public static EmbedBuilder getNasaInfo(String function, String date, String api) {
        Request request = null;
        EmbedBuilder embed = new EmbedBuilder();
        try {
            switch(function){
                case "apod":
                    if(date == null) {
                        request = new Request.Builder()
                                .url("https://api.nasa.gov/planetary/apod?api_key=" + api + "&count=1")
                                .build();
                    } else {
                        request = new Request.Builder()
                                .url("https://api.nasa.gov/planetary/apod?api_key=" + api + "&date=" + date)
                                .build();
                    }

                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            System.out.println("API call failed: " + response.code());
                            return null;
                        }

                        String jsonData = response.body().string();

                        JsonElement jsonElement = JsonParser.parseString(jsonData);
                        JsonObject json = jsonElement.isJsonArray()
                                ? jsonElement.getAsJsonArray().get(0).getAsJsonObject()
                                : jsonElement.getAsJsonObject();

                        String title = json.get("title").getAsString();
                        String explanation = json.get("explanation").getAsString();
                        String dateStr = json.get("date").getAsString();
                        String mediaType = json.get("media_type").getAsString();
                        String url = json.has("hdurl") ? json.get("hdurl").getAsString() : json.get("url").getAsString();

                        System.out.println("Media Type: " + mediaType);
                        System.out.println("Original URL: " + url);

                        embed.setTitle(title);
                        embed.setDescription(explanation);
                        embed.setFooter("📅 " + dateStr);
                        embed.setColor(new Color(25, 25, 112));

                        if (mediaType.equals("image")) {
                            String proxiedUrl = "https://images.weserv.nl/?n=-1&url=" + url.replaceFirst("https?://", "");

                            Request preloadRequest = new Request.Builder()
                                    .url(proxiedUrl)
                                    .build();
                            try (Response preloadResponse = client.newCall(preloadRequest).execute()) {
                                if (!preloadResponse.isSuccessful()) {
                                    System.out.println("Image preload failed: " + preloadResponse.code());
                                } else {
                                    System.out.println("Image preload successful");
                                }
                            } catch (Exception preloadException) {
                                preloadException.printStackTrace();
                            }

                            embed.setImage(proxiedUrl);
                        } else {
                            embed.addField("Media", "[Click here to view](" + url + ")", false);
                        }

                        return embed;
                    }

                case "mars weather":
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return embed;
    }
}
