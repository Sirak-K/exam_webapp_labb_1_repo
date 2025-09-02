package com.karis;

import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class WeatherClient extends BaseClient {

    private static final String BASE_URL
            = "https://opendata-download-metfcst.smhi.se/api/category/pmp3g/version/2/geotype/point";

    public WeatherClient(org.springframework.web.reactive.function.client.WebClient.Builder builder) {
        super(builder);
    }

    public String fetchForecast(double lon, double lat) {
        String url = UriComponentsBuilder.fromHttpUrl(BASE_URL)
                .pathSegment("lon", String.valueOf(lon), "lat", String.valueOf(lat), "data.json")
                .toUriString();

        return getJson(url);
    }
}
