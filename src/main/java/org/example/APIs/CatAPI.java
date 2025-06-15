package org.example.APIs;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.*;

public class CatAPI {
    private static final OkHttpClient client = new OkHttpClient();

    public static String getRandomCatImageUrl(String api) {
        try {
            Request request = new Request.Builder()
                    .url("https://api.thecatapi.com/v1/images/search")
                    .addHeader("x-api-key", api)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.out.println("API call failed: " + response.code());
                    return null;
                }

                String jsonData = response.body().string();
                JsonArray jsonArray = JsonParser.parseString(jsonData).getAsJsonArray();
                return jsonArray.get(0).getAsJsonObject().get("url").getAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
