package com.karis;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WeatherClient {

    private final RestTemplate restTemplate = new RestTemplate();

    private final String BASE_URL = "https://opendata-download-metfcst.smhi.se/api/category/pmp3g/version/2/geotype/point";

    public String fetchForecast(double lon, double lat) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .pathSegment("lon", String.valueOf(lon), "lat", String.valueOf(lat), "data.json")
                .toUriString();

        return restTemplate.getForObject(url, String.class);
    }
}
