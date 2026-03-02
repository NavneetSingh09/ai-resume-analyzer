package com.example.resume_analyzer.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenAIChatService {

    private final WebClient webClient;

    @Value("${openai.api.key}")
    private String apiKey;

    public String generateAnalysis(String prompt) {

        Map<String, Object> request = Map.of(
                "model", "gpt-4o-mini",
                "messages", new Object[]{
                        Map.of("role", "system", "content", "You are an expert technical recruiter."),
                        Map.of("role", "user", "content", prompt)
                }
        );

        Map response = webClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        var choices = (java.util.List<Map>) response.get("choices");
        var message = (Map) choices.get(0).get("message");

        return message.get("content").toString();
    }
}