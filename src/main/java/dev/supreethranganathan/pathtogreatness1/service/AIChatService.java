package dev.supreethranganathan.pathtogreatness1.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONPObject;
import dev.supreethranganathan.pathtogreatness1.model.ChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AIChatService {

    @Autowired
    private WebClient openAiWebClient;

    public String getChatResponse(String prompt) throws JsonProcessingException {

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                )
        );
        String response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(body)               // <-- this is critical
                .retrieve()
                .bodyToMono(String.class)
                .block();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        return root.get("choices")
                .get(0)
                .get("message")
                .get("content")
                .asText();
    }

    public String getChatResponse(ChatModel chatModel) throws JsonProcessingException {

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "user", "content", chatModel.prompt())
                )
        );
        String response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(body)               // <-- this is critical
                .retrieve()
                .bodyToMono(String.class)
                .block();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(response);
        return root.get("choices")
                .get(0)
                .get("message")
                .get("content")
                .asText();
    }

    public Flux<String> streamChat(String prompt) {
        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "stream", true,
                "messages", List.of(
//                        Map.of("role", "system", "content", "You are a senior AI engineering assistant. Be concise and factual. If unsure, say you don't know."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        ObjectMapper mapper = new ObjectMapper();

        return openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(body)
                .accept(MediaType.TEXT_EVENT_STREAM)        // tell OpenAI we want SSE
                .retrieve()
                .bodyToFlux(String.class)                   // we get SSE lines as Strings
                .map(line -> line.startsWith("data:") ? line.substring(5).trim() : line.trim())
                .filter(s -> !s.isBlank())
                .takeWhile(s -> !"[DONE]".equals(s))
                .map(json -> {
                    try {
                        JsonNode n = mapper.readTree(json);
                        // OpenAI streaming: choices[0].delta.content (may be null)
                        return n.path("choices").get(0).path("delta").path("content").asText("");
                    } catch (Exception e) {
                        return "";
                    }
                })
                .filter(chunk -> !chunk.isEmpty());
    }

}
