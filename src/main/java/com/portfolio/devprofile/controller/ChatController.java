package com.portfolio.devprofile.controller;


import com.portfolio.devprofile.dto.ChatRequest;
import com.portfolio.devprofile.dto.ChatResponse;
import com.portfolio.devprofile.rag.ChatRag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private RestClient restClient;
    private String ollamaModel;
    private RestClient restClientCohere;
    private String cohereModel;
    private ChatRag chatRag;

    public ChatController(@Value("${ollama.base-url}") String baseUrl,
                          @Value("${ollama.model.chat}") String model,
                          @Value("${cohere.api-key}") String cohereApiKey,
                          @Value("${cohere.base-url}") String cohereBaseUrl,
                          @Value("${cohere.model.chat}") String cohereModel,
                          ChatRag chatRag) {
        restClient = RestClient.builder().baseUrl(baseUrl).build();
        restClientCohere = RestClient.builder().baseUrl(cohereBaseUrl).defaultHeader("Authorization", "Bearer " + cohereApiKey).build();
        this.ollamaModel = model;
        this.cohereModel = cohereModel;
        this.chatRag = chatRag;
    }

    @PostMapping("/ollama")
    public ChatResponse chatPost(@RequestBody ChatRequest chatRequest){
        chatRequest.setModel(ollamaModel);
        System.out.println("Chat request: " + chatRequest);
        ChatResponse response = restClient.post()
                .uri("/api/chat")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(chatRequest)
                .retrieve()
                .body(ChatResponse.class);

        if(response == null){
            throw new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE,"Error calling model request");
        }

        return response;
    }

    @PostMapping("/cohere")
    public ChatResponse chatPostCohere(@RequestBody ChatRequest chatRequest){
        chatRequest.setModel(cohereModel);
        System.out.println("Chat request: " + chatRequest);



        ChatResponse response = restClientCohere.post()
                .uri("/v2/chat")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(chatRequest)
                .retrieve()
                .body(ChatResponse.class);

        if(response == null){
            throw new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE,"Error calling model request");
        }

        return response;
    }


    @PostMapping("/cohere/rag")
    public String chatRagCohere(@RequestBody ChatRequest chatRequest){
        System.out.println("Chat request: " + chatRequest);

        String response = chatRag.chatRequestCohere(chatRequest.getMessages().get(0).getContent() ,false);

        if(response == null){
            throw new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE,"Error calling model request");
        }

        return response;
    }





}
