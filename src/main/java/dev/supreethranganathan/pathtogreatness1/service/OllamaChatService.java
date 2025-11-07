package dev.supreethranganathan.pathtogreatness1.service;

import dev.supreethranganathan.pathtogreatness1.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class OllamaChatService {

    @Autowired
    private WebClient ollamaWebClient;

    @Value("${ollama.model}")
    private String model;

    public String getOllamaResponse(ChatModel prompt) {
        Map<String, Object> body = Map.of(
                "model", "mistral:7b",   // or mistral:7b-q4 if that’s what you pulled
                "prompt", prompt.prompt()
        );

        return ollamaWebClient.post()
                .uri("/api/generate")    // ✅ correct Ollama endpoint
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
