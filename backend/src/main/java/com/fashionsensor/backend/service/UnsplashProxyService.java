package com.fashionsensor.backend.service;

import io.netty.resolver.DefaultAddressResolverGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Service
public class UnsplashProxyService {

    private static final Logger logger = LoggerFactory.getLogger(UnsplashProxyService.class);

    /** Returned when Unsplash is unreachable — empty results so the layout renders without images. */
    private static final String EMPTY_RESULTS_JSON = "{\"results\":[],\"total\":0,\"total_pages\":0}";

    private final WebClient webClient;
    private final String unsplashAccessKey;

    public UnsplashProxyService(
            WebClient.Builder webClientBuilder,
            @Value("${unsplash.api.key:}") String unsplashAccessKey
    ) {
        // Use the JVM's default DNS resolver to avoid DnsNameResolverTimeoutException
        // in sandboxed/restricted network environments where Netty's async DNS fails.
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE);
        this.webClient = webClientBuilder
                .baseUrl("https://api.unsplash.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.unsplashAccessKey = unsplashAccessKey == null ? "" : unsplashAccessKey.trim();
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
                .onErrorResume(error -> {
                    System.out.println("Unsplash offline");
                    logger.warn("Unsplash network request failed — returning empty results. Cause: {}",
                            error.getMessage());
                    return Mono.just(EMPTY_RESULTS_JSON);
                });
    }
}
