package com.karis;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WeatherService {

    private final WeatherClient weatherClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherService(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    // Hämtar dagens väder (första datapunkten)
    public WeatherDTO getTodayForecast(double lon, double lat) {
        return extractForecast(lon, lat, 0);
    }

    // Hämtar gårdagens väder (ca -24h bakåt)
    public WeatherDTO getYesterdayForecast(double lon, double lat) {
        return extractForecast(lon, lat, -24);
    }

    // Hämtar morgondagens väder (ca 24h framåt)
    public WeatherDTO getTomorrowForecast(double lon, double lat) {
        return extractForecast(lon, lat, 24);
    }

    // Hämtar flera datapunkter (ex. 5 kommande timmar)
    public List<WeatherDTO> getForecast(double lon, double lat) {
        try {
            String rawJson = weatherClient.fetchForecast(lon, lat);
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode timeSeries = root.path("timeSeries");

            List<WeatherDTO> forecast = new ArrayList<>();
            for (int i = 0; i < Math.min(5, timeSeries.size()); i++) {
                JsonNode entry = timeSeries.get(i);
                String validTime = entry.path("validTime").asText();
                double temperature = extractTemperature(entry);
                forecast.add(new WeatherDTO(validTime, temperature));
            }
            return forecast;
        } catch (Exception e) {
            throw new RuntimeException("Kunde inte hämta forecast från SMHI", e);
        }
    }

    // Hjälpmetod för att plocka ut temperaturen
    private double extractTemperature(JsonNode entry) {
        for (JsonNode param : entry.path("parameters")) {
            if ("t".equals(param.path("name").asText())) {
                return param.path("values").get(0).asDouble();
            }
        }
        return Double.NaN; // fallback om inget hittas
    }

    // Hjälpmetod för att hämta en specifik datapunkt
    private WeatherDTO extractForecast(double lon, double lat, int offset) {
        try {
            String rawJson = weatherClient.fetchForecast(lon, lat);
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode timeSeries = root.path("timeSeries");

            if (timeSeries.isMissingNode() || timeSeries.size() == 0) {
                throw new RuntimeException("Ingen väderdata tillgänglig");
            }

            // Om offset är negativt → plocka första datapunkten men markera som gårdagens
            int index;
            if (offset < 0) {
                index = 0;
            } else {
                index = Math.min(offset, timeSeries.size() - 1);
            }

            JsonNode entry = timeSeries.get(index);

            String validTime = entry.path("validTime").asText();
            double temperature = extractTemperature(entry);

            if (offset < 0) {
                validTime = validTime + " (yesterday)";
            }

            return new WeatherDTO(validTime, temperature);

        } catch (Exception e) {
            throw new RuntimeException("Kunde inte hämta väderdata", e);
        }
    }
}
