package com.portfolio.devprofile.embedding;

import com.portfolio.devprofile.dto.ChatRequest;
import com.portfolio.devprofile.dto.OllamaEmbeddingRequest;
import com.portfolio.devprofile.dto.OllamaEmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class OllamaEmbeddingService implements EmbeddingServices{

    private final String model;
    private final RestClient restClient;

    public OllamaEmbeddingService(@Value("${ollama.base-url}") String baseUrl , @Value("${ollama.model.embedding}") String model){
        this.model = model;
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<Float> embed(String text) {
        OllamaEmbeddingRequest request = new OllamaEmbeddingRequest(model, text);
            OllamaEmbeddingResponse response = restClient.post()
                    .uri("api/embeddings")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OllamaEmbeddingResponse.class);

            if(response == null || response.embedding().size() != 768){
                throw new RuntimeException("ollama integration layer is broken");
            }
            return response.embedding();
    }

    public List<Float> embed(ChatRequest chatRequest) {
        OllamaEmbeddingResponse response = restClient.post()
                .uri("api/embeddings")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(chatRequest)
                .retrieve()
                .body(OllamaEmbeddingResponse.class);

        if(response == null || response.embedding().size() != 768){
            throw new RuntimeException("ollama integration layer is broken");
        }
        return response.embedding();
    }

}
