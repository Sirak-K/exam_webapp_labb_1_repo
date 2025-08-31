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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherService(WeatherClient weatherClient) {
        this.weatherClient = weatherClient;
    }

    // Hämtar dagens väder (≈ nu)
    public WeatherDTO getTodayForecast(double lon, double lat) {
        return extractForecast(lon, lat, 0);
    }

    // Hämtar gårdagens väder (≈ igår kl 12:00)
    public WeatherDTO getYesterdayForecast(double lon, double lat) {
        return extractYesterdayForecast(lon, lat);
    }

    // Hämtar morgondagens väder (~24h framåt i tiden)
    public WeatherDTO getTomorrowForecast(double lon, double lat) {
        return extractClosestForecast(lon, lat, 24);
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
        return Double.NaN;
    }

    // Hämtar en specifik datapunkt med index
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

    // Hämta närmaste datapunkt kring nu ± offset timmar
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

    // Hämtar gårdagens väder ≈ igår kl 12:00
    private WeatherDTO extractYesterdayForecast(double lon, double lat) {
        try {
            String rawJson = weatherClient.fetchForecast(lon, lat);
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode timeSeries = root.path("timeSeries");

            if (timeSeries.isMissingNode() || timeSeries.size() == 0) {
                throw new RuntimeException("Ingen väderdata tillgänglig");
            }

            LocalDateTime targetTime = LocalDate.now(ZoneOffset.UTC).minusDays(1).atTime(12, 0);

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
                throw new RuntimeException("Ingen matchande datapunkt hittades för gårdagen");
            }

            String validTime = bestEntry.path("validTime").asText();
            double temperature = extractTemperature(bestEntry);

            return new WeatherDTO(validTime, temperature);

        } catch (Exception e) {
            throw new RuntimeException("Kunde inte hämta gårdagens väder", e);
        }
    }
}
