package com.example.resume_analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
public class OpenAIChatService {

    private final WebClient webClient;
    private final String apiKey;

    public OpenAIChatService(
            WebClient webClient,
            @Value("${openai.api.key}") String apiKey
    ) {
        this.webClient = webClient.mutate()
                .baseUrl("https://api.openai.com/v1")
                .build();
        this.apiKey = apiKey;
    }

    public String generateAnalysis(String prompt) {

        Map<String, Object> request = Map.of(
                "model", "gpt-4o-mini",
                "messages", new Object[]{
                        Map.of("role", "system", "content", "You are an expert technical recruiter."),
                        Map.of("role", "user", "content", prompt)
                }
        );

        try {
            Map response = webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("choices")) {
                return "Analysis failed: empty response from OpenAI";
            }

            var choices = (java.util.List<Map>) response.get("choices");
            var message = (Map) choices.get(0).get("message");

            return message.get("content").toString();

        } catch (WebClientResponseException e) {
            return "Analysis failed: " + e.getStatusCode() + " - " + e.getMessage();
        } catch (Exception e) {
            return "Analysis failed: " + e.getMessage();
        }
    }
}