package com.portfolio.devprofile.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Setter
@Getter
public class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private boolean stream;
}

