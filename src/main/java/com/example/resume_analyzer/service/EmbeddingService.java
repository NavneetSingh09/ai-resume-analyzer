package com.example.resume_analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final WebClient webClient;
    private final String apiKey;

    public EmbeddingService(
            WebClient webClient,
            @Value("${openai.api.key}") String apiKey
    ) {
        this.webClient = webClient.mutate()
                .baseUrl("https://api.openai.com/v1")
                .build();
        this.apiKey = apiKey;
    }

    public List<Double> getEmbedding(String text) {

        Map<String, Object> requestBody = Map.of(
                "model", "text-embedding-3-small",
                "input", text
        );

        Map response = webClient.post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (response == null || !response.containsKey("data")) {
            return List.of();
        }

        List<Map> data = (List<Map>) response.get("data");
        Map embeddingObject = data.get(0);

        return (List<Double>) embeddingObject.get("embedding");
    }
}