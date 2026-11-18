package com.portfolio.devprofile.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Setter
@Getter
//@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private Boolean stream;
//    private Double temperature = 0.2;
//    @JsonProperty("max_tokens")
//    private Integer maxTokens = 250;

    public ChatRequest(String model, List<ChatMessage> messages, boolean stream) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
//        this.temperature = 0.2;
//        this.maxTokens = 250;
    }

    @Override
    public String toString() {

        for(ChatMessage message : messages) {
            System.out.println("Message: " + message.getRole() + " - " + message.getContent());
        }

        return "ChatRequest{" +
                "model='" + model + '\'' +
                ", stream=" + stream +
//                ", temperature=" + temperature +
//                ", maxTokens=" + maxTokens +
                '}';
    }
}

