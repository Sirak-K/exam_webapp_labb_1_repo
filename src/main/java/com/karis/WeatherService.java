package com.karis;

import org.springframework.stereotype.Service;

@Service
public class WeatherService {

    private final ForecastService forecastService;
    private final ObservationService observationService;

    public WeatherService(ForecastService forecastService, ObservationService observationService) {
        this.forecastService = forecastService;
        this.observationService = observationService;
    }

    // Delegation: logiken är oförändrad
    public WeatherDTO getTodayForecast(double lon, double lat) {
        return forecastService.getTodayForecast(lon, lat);
    }

    public WeatherDTO getTomorrowForecast(double lon, double lat) {
        return forecastService.getTomorrowForecast(lon, lat);
    }

    public WeatherDTO getYesterdayObservation() {
        return observationService.getYesterdayObservation();
    }
}
