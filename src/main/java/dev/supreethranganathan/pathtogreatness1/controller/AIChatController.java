package dev.supreethranganathan.pathtogreatness1.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.supreethranganathan.pathtogreatness1.model.ChatModel;
import dev.supreethranganathan.pathtogreatness1.service.AIChatService;
import dev.supreethranganathan.pathtogreatness1.service.OllamaChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ai-chat")
public class AIChatController {

    @Autowired
    private AIChatService aiChatService;

    @Autowired
    private OllamaChatService ollamaChatService;


    @PostMapping("/ask")
    public String askQuestion(@RequestParam("question") String question) throws JsonProcessingException {
        return aiChatService.getChatResponse(question);
    }

    @PostMapping("/prompt")
    public String askPrompt(@RequestBody ChatModel chatModel) throws JsonProcessingException {
        return aiChatService.getChatResponse(chatModel);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestBody ChatModel chatModel) {
        return aiChatService.streamChat(chatModel.prompt());
    }

    @PostMapping("/ollama-stream-chat")
    public Flux<String> askOllama(@RequestBody ChatModel chatModel) throws JsonProcessingException {

        return ollamaChatService.getOllamaResponse(chatModel);
    }

}
