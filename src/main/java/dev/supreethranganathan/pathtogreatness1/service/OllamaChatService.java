package dev.supreethranganathan.pathtogreatness1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.supreethranganathan.pathtogreatness1.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
public class OllamaChatService {

    @Autowired
    private WebClient ollamaWebClient;
    @Autowired
    WebRetrievalService retriever;

    @Autowired
    ObjectMapper objectMapper;

    @Value("${ollama.model}")
    private String model;

    public String getOllamaResponse(ChatModel prompt) throws JsonProcessingException {
        Map<String, Object> body = Map.of(
                "model", "mistral",   // or mistral:7b-q4 if that’s what you pulled
                "prompt", prompt.prompt(),
                "stream",false
        );

        String response = ollamaWebClient.post()
                .uri("/api/generate")    // ✅ correct Ollama endpoint
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        JsonNode root = objectMapper.readTree(response);
        return root.get("response").asText();
    }

    public Flux<String> getStreamingOllamaResponse(ChatModel chatModel) {
        Map<String, Object> body = Map.of(
                "model", "mistral",
                "prompt", chatModel.prompt(),
                "stream", true
        );

        return ollamaWebClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .flatMap(line -> {
                    try {
                        JsonNode json = objectMapper.readTree(line);
                        if (json.has("response")) {
                            return Flux.just(json.get("response").asText());
                        }
                    } catch (Exception ignored) {}
                    return Flux.empty();
                })
                .filter(s -> !s.isBlank())
                .doOnSubscribe(sub -> System.out.println("🟢 Ollama stream started"))
                .doFinally(sig -> System.out.println("🟣 Ollama stream ended"));
    }

    public String askWithWeb(String question) throws JsonProcessingException {
        List<String> snippets = retriever.retrieveSnippets(question);
        String context = String.join("\n\n", snippets);

        String fullPrompt = "Use the following web info to answer:\n" +
                context + "\n\nQuestion: " + question;

        return getOllamaResponse(new ChatModel(fullPrompt));
    }

}
