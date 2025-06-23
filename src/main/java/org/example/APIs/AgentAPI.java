package org.example.APIs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;

public class AgentAPI {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(Duration.ofSeconds(15))
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(15))
            .build();

    private static final String SERVICE_URL = "http://localhost:8000/agent";

    public static class AgenticAiResponse {
        public final String intent;
        public final String response;
        public final String url;

        public AgenticAiResponse(String intent, String response, String url) {
            this.intent = intent;
            this.response = response;
            this.url = url;
        }
    }

    public static AgenticAiResponse queryDeepseek(String userMessage) {
        // Create JSON body
        JsonObject body = new JsonObject();
        body.addProperty("message", userMessage);

        Request request = new Request.Builder()
                .url(SERVICE_URL)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.out.println("Deepseek API call failed: " + response.code());
                return null;
            }

            String jsonData = response.body().string();
            JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();

            String intent = jsonObject.has("intent") && !jsonObject.get("intent").isJsonNull()
                    ? jsonObject.get("intent").getAsString()
                    : "none";

            String reply = jsonObject.has("response") && !jsonObject.get("response").isJsonNull()
                    ? jsonObject.get("response").getAsString()
                    : null;

            String url = jsonObject.has("url") && !jsonObject.get("url").isJsonNull()
                    ? jsonObject.get("url").getAsString()
                    : null;

            return new AgenticAiResponse(intent, reply, url);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
