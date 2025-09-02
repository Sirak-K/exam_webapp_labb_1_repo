package com.karis;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ForecastService {

    private final WeatherClient weatherClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ForecastService(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    // HÄMTA PROGNOS för Dagens
    public WeatherDTO getTodayForecast(double lon, double lat) {
        return extractForecast(lon, lat, 0);
    }

    // HÄMTA PROGNOS för Morgondagen
    public WeatherDTO getTomorrowForecast(double lon, double lat) {
        return extractClosestForecast(lon, lat, 24);
    }

    // Hjälpmetod för Forecast-datapunkt
    // 1. Hämtar en specifik prognospunkt från SMHI:s timeSeries-lista, baserat på ett givet index.
    // 2. Använder extractTemperature(entry) för att hämta temperaturen (t) från JSON-datan.
    private WeatherDTO extractForecast(double lon, double lat, int index) {
        try {
            String rawJson = weatherClient.fetchForecast(lon, lat);
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode timeSeries = root.path("timeSeries");

            if (timeSeries.isMissingNode() || timeSeries.size() == 0) {
                throw new RuntimeException("Ingen väderdata tillgänglig");
            }

            index = Math.max(0, Math.min(index, timeSeries.size() - 1));
            JsonNode entry = timeSeries.get(index);

            String validTime = entry.path("validTime").asText();
            double temperature = extractTemperature(entry);

            return new WeatherDTO(validTime, temperature);

        } catch (Exception e) {
            throw new RuntimeException("Kunde inte hämta väderdata", e);
        }
    }

    // Hjälpmetod: Forecast-datapunkt ≈ nu ± offset timmar
    // 1. Hämtar den prognospunkt i timeSeries som ligger närmast en viss tidpunkt (nu + hourOffset).
    // 2. Även här används extractTemperature(entry) för att hämta temperaturen (t) från JSON-datan.
    private WeatherDTO extractClosestForecast(double lon, double lat, int hourOffset) {
        try {
            String rawJson = weatherClient.fetchForecast(lon, lat);
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode timeSeries = root.path("timeSeries");

            if (timeSeries.isMissingNode() || timeSeries.size() == 0) {
                throw new RuntimeException("Ingen väderdata tillgänglig");
            }

            LocalDateTime targetTime = LocalDateTime.now(ZoneOffset.UTC).plusHours(hourOffset);

            JsonNode bestEntry = null;
            long bestDiff = Long.MAX_VALUE;

            for (JsonNode entry : timeSeries) {
                LocalDateTime vt = LocalDateTime.parse(entry.path("validTime").asText().replace("Z", ""));
                long diff = Math.abs(Duration.between(targetTime, vt).toHours());
                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestEntry = entry;
                }
            }

            if (bestEntry == null) {
                throw new RuntimeException("Ingen matchande datapunkt hittades");
            }

            String validTime = bestEntry.path("validTime").asText();
            double temperature = extractTemperature(bestEntry);

            return new WeatherDTO(validTime, temperature);

        } catch (Exception e) {
            throw new RuntimeException("Kunde inte hämta väderdata", e);
        }
    }

// En beroendemetod som gör själva temperatur-utläsningen
// De andra två sköter vilken datapunkt i tidsserien som ska användas
    private double extractTemperature(JsonNode entry) {
        for (JsonNode param : entry.path("parameters")) {
            if ("t".equals(param.path("name").asText())) {
                return param.path("values").get(0).asDouble();
            }
        }
        return Double.NaN;
    }
}
