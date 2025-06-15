package org.example.APIs;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.*;


public class WeatherAPI {
    private static final OkHttpClient client = new OkHttpClient();
    public static String getWeatherInfo(String city, String api) {
        try {
            Request request = new Request.Builder()
                    .url("http://api.weatherstack.com/current?access_key=" + api + "&query=" + city)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.out.println("API call failed: " + response.code());
                    return null;
                }

                String jsonData = response.body().string();
                JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();

                JsonObject location = jsonObject.getAsJsonObject("location");
                JsonObject current = jsonObject.getAsJsonObject("current");

                String name = location.get("name").getAsString();
                String country = location.get("country").getAsString();
                String region = location.get("region").getAsString();
                int temperature = current.get("temperature").getAsInt();
                return String.format("in %s, %s, %s\n**it's currently %d°C**", name, region, country, temperature);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
