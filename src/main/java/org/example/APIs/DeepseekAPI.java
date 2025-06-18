package org.example.APIs;

import net.dv8tion.jda.api.EmbedBuilder;
import okhttp3.*;
import com.google.gson.*;

import java.awt.*;
import java.io.IOException;
import java.time.Duration;

public class DeepseekAPI {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(15)) // total time for the call
            .connectTimeout(Duration.ofSeconds(10)) // time to connect to the server
            .readTimeout(Duration.ofSeconds(15)) // time to read the server response
            .build();
    private static final String model = "deepseek/deepseek-chat";

    public static String getResponse(String prompt, String api, String query) {
        prompt = switch (query) {
            case "mention" -> "Imagine you are a human with self irony, capable of making witty remarks, answer concisely. Now, reply to this: " + prompt;
            case "weather" -> "Imagine you are a weather forecaster with self irony, capable of making witty remarks, answer concisely. Now talk about the following, as if you are currently live: " + prompt;
            default -> "";
        };
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);


        Request request = new Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + api)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(
                        body.toString(), MediaType.parse("application/json")))
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("API call failed: " + response.code());
                return null;
            }

            String jsonData = response.body().string();
            JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();

            JsonArray choices = jsonObject.getAsJsonArray("choices");
            if (choices != null && choices.size() > 0) {
                JsonObject messageObj = choices.get(0)
                        .getAsJsonObject()
                        .getAsJsonObject("message");
                return messageObj.get("content").getAsString();
            }

            return "No response";
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}