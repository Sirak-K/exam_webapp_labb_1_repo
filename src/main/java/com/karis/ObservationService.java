package com.karis;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ObservationService {

    private final ObservationClient observationClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ObservationService(ObservationClient observationClient) {
        this.observationClient = observationClient;
    }

    // Hämtar JSON-data via observationClient.fetchObservations().
    // - Försöker hitta en observation som matchar igår via findClosestObservation()
    // -- Om ingen hittas så försöker den med dagen innan. Om dessa två scenarion ej lyckas så används den senaste tillgängliga observationen.
    public WeatherDTO getYesterdayObservation() {
        try {
            String rawJson = observationClient.fetchObservations();
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode data = root.path("value");

            if (data.isMissingNode() || !data.isArray()) {
                throw new RuntimeException("Ingen observationsdata tillgänglig");
            }

            LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
            WeatherDTO yesterdayData = findClosestObservation(data, yesterday, "Observation igår");
            if (yesterdayData != null) {
                return yesterdayData;
            }

            LocalDate dayBeforeYesterday = LocalDate.now(ZoneOffset.UTC).minusDays(2);
            WeatherDTO fallbackData = findClosestObservation(data, dayBeforeYesterday, "Observation förrgår");
            if (fallbackData != null) {
                return fallbackData;
            }

            JsonNode lastEntry = data.get(data.size() - 1);
            return parseObservationEntry(lastEntry, "Senaste observation");

        } catch (Exception e) {
            throw new RuntimeException("Kunde inte hämta gårdagens observationer: " + e.getMessage(), e);
        }
    }

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

        return bestEntry == null ? null : parseObservationEntry(bestEntry, label);
    }

// Här konverteras en JSON-punkt till WeatherDTO med tid + temp.
    private WeatherDTO parseObservationEntry(JsonNode entry, String label) {
        long ms = entry.path("date").asLong();
        LocalDateTime obsTime = LocalDateTime.ofEpochSecond(ms / 1000, 0, ZoneOffset.UTC);
        double temperature = entry.path("value").asDouble();
        return new WeatherDTO(obsTime.toString() + " (" + label + ")", temperature);
    }
}
