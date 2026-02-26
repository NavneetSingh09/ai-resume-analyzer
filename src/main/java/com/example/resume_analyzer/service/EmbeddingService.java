package com.example.resume_analyzer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {

    private final WebClient webClient;

    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    public EmbeddingService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    public List<Double> getEmbedding(String text) {

        Map<String, Object> requestBody = Map.of(
                "model", "text-embedding-3-small",
                "input", text
        );

        Map response = webClient.post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + System.getenv("OPENAI_API_KEY"))
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map> data = (List<Map>) response.get("data");
        Map embeddingObject = data.get(0);

        return (List<Double>) embeddingObject.get("embedding");
    }
}