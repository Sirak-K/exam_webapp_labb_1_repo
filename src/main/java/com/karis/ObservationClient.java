package com.karis;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ObservationClient {

    private final RestTemplate restTemplate = new RestTemplate();

// Bromma flygplats (98230)
    private final String BASE_URL
            = "https://opendata-download-metobs.smhi.se/api/version/1.0/parameter/1/station/98230/period/latest-months/data.json";

    public String fetchObservations() {
        return restTemplate.getForObject(BASE_URL, String.class);
    }
}
