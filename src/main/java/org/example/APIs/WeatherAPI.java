package org.example.APIs;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.gson.*;

import static org.example.APIs.DeepseekAPI.getResponse;


public class WeatherAPI {
    private static final OkHttpClient client = new OkHttpClient();
    public static String getWeatherInfo(String city, String apiDeepseek, String apiWeather) {
        try {
            Request request = new Request.Builder()
                    .url("http://api.weatherstack.com/current?access_key=" + apiWeather + "&query=" + city)
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
                String condition = current.get("weather_descriptions").getAsString();
                String localTime = location.get("localtime").getAsString();
                int temperature = current.get("temperature").getAsInt();
                String output = getResponse(String.format("in %s, %s, %s it's currently %d°C, %s, local time: %s", name, region, country, temperature, condition, localTime), apiDeepseek, "weather");
                if (output == null) {
                    System.err.println("DeepSeek returned null for prompt");
                }
                return output;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
