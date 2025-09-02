package com.karis;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

// Väderdata: Observation
    @GetMapping("/api/weather/yesterday")
    public WeatherDTO yesterday() {
        return weatherService.getYesterdayObservation();
    }

// Väderdata: Forecast
    @GetMapping("/api/weather/today")
    public WeatherDTO today(@RequestParam double lon, @RequestParam double lat) {
        return weatherService.getTodayForecast(lon, lat);
    }

// Väderdata: Forecast
    @GetMapping("/api/weather/tomorrow")
    public WeatherDTO tomorrow(@RequestParam double lon, @RequestParam double lat) {
        return weatherService.getTomorrowForecast(lon, lat);
    }
}
