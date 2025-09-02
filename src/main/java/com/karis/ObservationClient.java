package com.karis;

import org.springframework.stereotype.Component;

@Component
public class ObservationClient extends BaseClient {

    // Bromma flygplats (98230)
    private static final String BASE_URL
            = "https://opendata-download-metobs.smhi.se/api/version/1.0/parameter/1/station/98230/period/latest-months/data.json";

    public ObservationClient(org.springframework.web.reactive.function.client.WebClient.Builder builder) {
        super(builder);
    }

    public String fetchObservations() {
        return getJson(BASE_URL);
    }
}
