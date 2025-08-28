package com.karis;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather/today")
    public WeatherDTO today(@RequestParam double lon, @RequestParam double lat) {
        return weatherService.getTodayForecast(lon, lat);
    }

    @GetMapping("/weather/tomorrow")
    public WeatherDTO tomorrow(@RequestParam double lon, @RequestParam double lat) {
        return weatherService.getTomorrowForecast(lon, lat);
    }

    @GetMapping("/weather/forecast")
    public List<WeatherDTO> forecast(@RequestParam double lon, @RequestParam double lat) {
        return weatherService.getForecast(lon, lat);
    }
}
