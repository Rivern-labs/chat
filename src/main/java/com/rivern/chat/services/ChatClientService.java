package com.rivern.chat.services;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;

@Service
public class ChatClientService {
    private final ChatClient chatClient;

    public ChatClientService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String getResponse(List<Message> messageHistory) {
        Prompt prompt = new Prompt(messageHistory);
        ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
        return response.getResult().getOutput().getText();
    }

    public <T> T getResponse(List<Message> messageHistory, Class<T> responseType) {
        Prompt prompt = new Prompt(messageHistory);
        T response = chatClient.prompt(prompt).call().entity(responseType);

        return response;
    }

    public String getStatelessResponse(String userPrompt) {
        List<Message> messageHistory = List.of(new UserMessage(userPrompt));
        Prompt prompt = new Prompt(messageHistory);
        return chatClient.prompt(prompt).call().chatResponse().getResult().getOutput().getText();
    }

    public Flux<String> getStreamingResponse(List<Message> messages) {
        Prompt prompt = new Prompt(messages);
        return chatClient.prompt(prompt).stream().content();
    }
}
