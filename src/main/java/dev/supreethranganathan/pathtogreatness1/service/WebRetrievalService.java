package dev.supreethranganathan.pathtogreatness1.service;

import dev.supreethranganathan.pathtogreatness1.model.TavilyResponse;
import dev.supreethranganathan.pathtogreatness1.model.TavilyResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class WebRetrievalService {

    @Value("${tavily.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.create("https://api.tavily.com");

    public List<String> retrieveSnippets(String query) {
        Map<String, Object> body = Map.of(
                "api_key", apiKey,
                "query", query,
                "num_results", 3
        );

        TavilyResponse response = webClient.post()
                .uri("/search")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(TavilyResponse.class)
                .block();

        return response.results().stream()
                .map(TavilyResult::content)
                .toList();
    }
}
