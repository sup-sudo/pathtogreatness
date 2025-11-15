package dev.supreethranganathan.pathtogreatness1.controller;

import dev.supreethranganathan.pathtogreatness1.model.ChatModel;
import dev.supreethranganathan.pathtogreatness1.service.OllamaSmartChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/ollama/smart-chat")
public class OllamaSmartChatController {

    @Autowired
    OllamaSmartChatService ollamaSmartChatService;

    @PostMapping("/ask")
    public Flux<String> askQuestion(@RequestBody ChatModel question) {
        return ollamaSmartChatService.getSmartOllamaResponse(question);
    }


}
