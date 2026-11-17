package com.portfolio.devprofile.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CohereEmbeddingResponse(Embeddings embeddings,
                                      @JsonProperty("response_type")
                                      String responseType) {
    public record Embeddings(
            @JsonProperty("float")
            List<List<Float>> vectors
    ) {}

    public List<Float> getEmbeddings() {
        return embeddings.vectors.get(0);
    }
}
