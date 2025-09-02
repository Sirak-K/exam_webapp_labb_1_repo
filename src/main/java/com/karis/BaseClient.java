package com.karis;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * BaseClient - gemensam HTTP-klient för SMHI-API. Använder blockerande för
 * minimal kodändring.
 */
@Component
public abstract class BaseClient {

    protected final WebClient webClient;

    public BaseClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    /**
     * Hjälpmetod för att hämta JSON-data från en URL.
     *
     * @param url - API endpoint
     * @return response body som String
     */
    protected String getJson(String url) {
        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block(); // block för att bibehålla samma synkrona flöde som RestTemplate
    }
}
