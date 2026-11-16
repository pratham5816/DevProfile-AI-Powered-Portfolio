package com.portfolio.devprofile.vectorstore;

import com.portfolio.devprofile.dto.SearchRequest;
import com.portfolio.devprofile.dto.SearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;

@Service
public class QdrantVectorStore {

    private final RestClient restClient;
    private final String collectionName;

    public QdrantVectorStore(@Value("${qdrant.url}") String baseUrl, @Value("${qdrant.api-key}")  String apiKey, @Value("${qdrant.collection-name}") String collectionName) {
        this.collectionName = collectionName;
        this.restClient = RestClient.builder().baseUrl(baseUrl).defaultHeader("api-key",apiKey).build();
    }

    public void upsertPoints(List<Point> pointList) {
        if(pointList == null || pointList.isEmpty()) {
            throw new IllegalArgumentException("List of points cannot be null or empty");
        }
        try {
            ResponseEntity<String> response = restClient.put()
                    .uri("/collections/{collectionName}/points?wait=true", collectionName)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new UpsertPointsRequest(pointList))
                    .retrieve()
                    .toEntity(String.class);

            System.out.println(response.getBody());
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseEntity<String> checkCollection(){
        return restClient.get()
                    .uri("/collections")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                   .toEntity(String.class);
    }

    public SearchResponse searchPoints(List<Float> vector, int limit) {

        if(vector == null || vector.isEmpty()) {
            throw new IllegalArgumentException("List of points cannot be null or empty");
        }

        SearchRequest searchRequest = new SearchRequest(vector, limit, true);

        return restClient.post()
                .uri("collections/{collectionsName}/points/search", collectionName)
                .accept(MediaType.APPLICATION_JSON)
                .body(searchRequest)
                .retrieve()
                .body(SearchResponse.class);
    }
}
