package com.fashionsensor.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class UnsplashProxyService {

    private static final Logger logger = LoggerFactory.getLogger(UnsplashProxyService.class);
    private final WebClient webClient;
    private final String unsplashAccessKey;

    public UnsplashProxyService(
            WebClient.Builder webClientBuilder,
            @Value("${unsplash.api.key}") String unsplashAccessKey
    ) {
        this.webClient = webClientBuilder.baseUrl("https://api.unsplash.com").build();
        this.unsplashAccessKey = unsplashAccessKey;
    }

    public Mono<String> searchPhotos(String query, int perPage, String orientation) {
        logger.info("Proxying Unsplash search request: query={}, perPage={}", query, perPage);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/photos")
                        .queryParam("query", query)
                        .queryParam("per_page", perPage)
                        .queryParam("orientation", orientation)
                        .queryParam("client_id", unsplashAccessKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> logger.error("Error calling Unsplash API", error));
    }
}
