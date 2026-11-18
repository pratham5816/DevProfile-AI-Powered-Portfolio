package com.portfolio.devprofile.rag;

import com.portfolio.devprofile.dto.*;
import com.portfolio.devprofile.embedding.CohereEmbeddingService;
import com.portfolio.devprofile.embedding.OllamaEmbeddingService;
import com.portfolio.devprofile.vectorstore.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


import java.util.List;

@Service
public class ChatRag {
    private RestClient restClient;
    private RestClient restClientCohere;
    private String cohereModel;
    private String model;
    private String cohereSystemMessage;
    private OllamaEmbeddingService ollamaEmbeddingService;
    private QdrantVectorStore qdrantVectorStore;
    private int limit;
    private CohereEmbeddingService cohereEmbeddingService;

    public ChatRag(@Value("${ollama.model.chat}") String model,
                   @Value("${ollama.base-url}") String baseUrl,
                   @Value("${qdrant.document.limit}") int limit,
                   @Autowired OllamaEmbeddingService ollamaEmbeddingService,
                   @Autowired QdrantVectorStore qdrantVectorStore,
                   @Value("${cohere.api-key}") String cohereApiKey,
                   @Value("${cohere.base-url}") String cohereBaseUrl,
                   @Value("${cohere.model.chat}") String cohereModel,
                   @Value("${cohere.model.chat.systemMessage}") String cohereSystemMessage,
                   @Autowired CohereEmbeddingService cohereEmbeddingService) {
         this.restClient = RestClient.builder().baseUrl(baseUrl).build();
         this.restClientCohere = RestClient.builder().baseUrl(cohereBaseUrl).defaultHeader("Authorization", "Bearer " + cohereApiKey).build();
         this.model = model;
         this.cohereModel = cohereModel;
         this.ollamaEmbeddingService = ollamaEmbeddingService;
         this.qdrantVectorStore = qdrantVectorStore;
         this.limit = limit;
         this.cohereSystemMessage = cohereSystemMessage;
         this.cohereEmbeddingService = cohereEmbeddingService;
    }


    public ChatResponse chatModel(ChatMessage message , boolean stream) {
        ChatRequest request = new ChatRequest(model , List.of(message) , stream);
       return restClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ChatResponse.class);
    }

    public String chatRequestCohere(String userQuery ,  boolean stream) {

        if(userQuery == null){
            throw new IllegalArgumentException("userQuery cannot be null");
        }

        List<Float> vector = cohereEmbeddingService.embed(userQuery , "search_query");

        if(vector == null){
            throw new IllegalArgumentException("problem converting user input");
        }

        SearchResponse response = qdrantVectorStore.searchPoints(vector, limit);

        List<ScoredPoint> points = response.result();

        StringBuilder content =  new StringBuilder(userQuery + "\n\n" + "Context from the documents via vector db: " + "\n\n");

        for(ScoredPoint point : points){
            content.append(point.payload().get("content") + "\n\n");
        }

        System.out.println("------------------------------------------------");
        System.out.println("context for llm with query " + content.toString());
        System.out.println("------------------------------------------------");

        ChatRequest chatRequest = new ChatRequest(
                cohereModel,
                List.of(new ChatMessage("system", cohereSystemMessage) , new ChatMessage("user", content.toString())) , stream);

        String temp = chatRequest.toString();
        System.out.println("============= " + temp);
        ChatResponse chatResponse = restClientCohere.post()
                .uri("/v2/chat")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(chatRequest)
                .retrieve()
                .body(ChatResponse.class);


        if(chatResponse == null){
            throw new IllegalArgumentException("problem calling cohere api");
        }

//        System.out.println(chatResponse.message().toString());

        return chatResponse.text();

    }
}
