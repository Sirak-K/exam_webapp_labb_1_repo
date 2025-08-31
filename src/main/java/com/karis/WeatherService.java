package com.karis;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WeatherService {

    private final WeatherClient weatherClient;
    private final ObservationClient observationClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherService(WeatherClient weatherClient, ObservationClient observationClient) {
        this.weatherClient = weatherClient;
        this.observationClient = observationClient;
    }

    // Hämtar dagens väder (forecast ≈ nu)
    public WeatherDTO getTodayForecast(double lon, double lat) {
        return extractForecast(lon, lat, 0);
    }

    // Hämtar morgondagens väder (forecast ≈ nu+24h)
    public WeatherDTO getTomorrowForecast(double lon, double lat) {
        return extractClosestForecast(lon, lat, 24);
    }

    // Hämtar gårdagens väder (observation igår eller fallback förrgår)
    // Hämtar gårdagens väder (fallback till förrgår, annars senaste tillgängliga)
    public WeatherDTO getYesterdayObservation() {
        try {
            String rawJson = observationClient.fetchObservations();
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode data = root.path("value");

            if (data.isMissingNode() || !data.isArray()) {
                throw new RuntimeException("Ingen observationsdata tillgänglig");
            }

            // 1. Försök igår
            LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
            WeatherDTO yesterdayData = findClosestObservation(data, yesterday, "Observation igår");
            if (yesterdayData != null) {
                return yesterdayData;
            }

            // 2. Försök förrgår
            LocalDate dayBeforeYesterday = LocalDate.now(ZoneOffset.UTC).minusDays(2);
            WeatherDTO fallbackData = findClosestObservation(data, dayBeforeYesterday, "Observation förrgår");
            if (fallbackData != null) {
                return fallbackData;
            }

            // 3. Annars ta senaste tillgängliga
            JsonNode lastEntry = data.get(data.size() - 1);
            long ms = lastEntry.path("date").asLong();
            LocalDateTime obsTime = LocalDateTime.ofEpochSecond(ms / 1000, 0, ZoneOffset.UTC);
            double temperature = lastEntry.path("value").asDouble();
            return new WeatherDTO(obsTime.toString() + " (Senaste observation)", temperature);

        } catch (Exception e) {
            throw new RuntimeException("Kunde inte hämta gårdagens observationer: " + e.getMessage(), e);
        }
    }

    // Hjälpmetod för observationer
    private WeatherDTO findClosestObservation(JsonNode data, LocalDate targetDate, String label) {
        JsonNode bestEntry = null;
        long bestDiff = Long.MAX_VALUE;

        for (JsonNode entry : data) {
            if (entry.path("value").isMissingNode() || entry.path("value").isNull()) {
                continue;
            }
            long ms = entry.path("date").asLong();
            LocalDateTime obsTime = LocalDateTime.ofEpochSecond(ms / 1000, 0, ZoneOffset.UTC);

            long diff = Math.abs(Duration.between(obsTime.toLocalDate().atStartOfDay(), targetDate.atStartOfDay()).toHours());
            if (diff < bestDiff) {
                bestDiff = diff;
                bestEntry = entry;
            }
        }

        if (bestEntry == null) {
            return null;
        }

        long ms = bestEntry.path("date").asLong();
        LocalDateTime obsTime = LocalDateTime.ofEpochSecond(ms / 1000, 0, ZoneOffset.UTC);
        double temperature = bestEntry.path("value").asDouble();

        // Lägg till label (igår/förrgår)
        return new WeatherDTO(obsTime.toString() + " (" + label + ")", temperature);
    }

    // Hämtar flera datapunkter (forecast ex. 5 timmar framåt)
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

    // Hjälpmetod för temperatur från forecast-API
    private double extractTemperature(JsonNode entry) {
        for (JsonNode param : entry.path("parameters")) {
            if ("t".equals(param.path("name").asText())) {
                return param.path("values").get(0).asDouble();
            }
        }
        return Double.NaN;
    }

    // Forecast datapunkt via index
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

    // Forecast datapunkt ≈ nu ± offset timmar
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
}
