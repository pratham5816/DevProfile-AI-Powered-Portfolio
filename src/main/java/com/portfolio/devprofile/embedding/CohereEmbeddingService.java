package com.portfolio.devprofile.embedding;

import com.portfolio.devprofile.dto.CohereEmbeddingRequest;
import com.portfolio.devprofile.dto.CohereEmbeddingResponse;
import com.portfolio.devprofile.dto.Document;
import com.portfolio.devprofile.ingestion.Parser;
import com.portfolio.devprofile.vectorstore.Point;
import com.portfolio.devprofile.vectorstore.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class CohereEmbeddingService {

    private final RestClient restClient;
    private final String model;
    private final Parser parser;
    private final QdrantVectorStore qdrantVectorStore;


    public CohereEmbeddingService(@Value("${cohere.base-url}") String baseUrl,
                                  @Value("${cohere.model.embed}") String model,
                                  @Value("${cohere.api-key}") String apiKey,
                                  @Autowired Parser parser,
                                  @Autowired QdrantVectorStore qdrantVectorStore) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).defaultHeader("Authorization","Bearer " + apiKey).build();
        this.model = model;
        this.parser = parser;
        this.qdrantVectorStore = qdrantVectorStore;
    }

    public List<Float> embed(String text , String inputType) {
        List<String> embeddingTypes = List.of("float");
        CohereEmbeddingRequest request = new CohereEmbeddingRequest(model, List.of(text) , inputType , embeddingTypes);

        CohereEmbeddingResponse response =  restClient.post()
                .uri("v2/embed")
                .body(request)
                .retrieve()
                .body(CohereEmbeddingResponse.class);

        System.out.println("Cohere embedding started");
        if(response == null || response.embeddings() == null || response.getEmbeddings().isEmpty()){
            throw new RuntimeException("Cohere embedding service failed");
        }
        return response.getEmbeddings();
    }

    public List<List<Float>> embedBatch(List<String> texts, String inputType) {
        List<String> embeddingTypes = List.of("float");
        CohereEmbeddingRequest request = new CohereEmbeddingRequest(model, texts, inputType, embeddingTypes);
        CohereEmbeddingResponse response = restClient.post()
                .uri("v2/embed")
                .body(request)
                .retrieve()
                .body(CohereEmbeddingResponse.class);
        if (response == null || response.embeddings() == null || response.embeddings().vectors().isEmpty()) {
            throw new RuntimeException("Cohere batch embedding failed");
        }
        return response.embeddings().vectors(); // Returns List of 50 vector lists
    }

    public void runIngestion(){
        try{
            InputStream stream = getClass().getResourceAsStream("/data/work_portfolio.csv");
            List<Document> documents = parser.parseDocuments(stream);
            List<Point> points = new ArrayList<>();
            int count = 0;
            for(Document document : documents){
                count++;
                if(count >= 100){
                    System.out.println("Sleeping for 10 seconds to avoid rate limit");
                    Thread.sleep(10000);
                    count = 0;
                }
                Map<String, Object> payload = Map.of("content" , document.content() , "title" , document.title());
                List<Float> list = embed(document.content(), "search_document");
                points.add(new Point(document.id(), list , payload));
            }
            qdrantVectorStore.upsertPoints(points);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void runIngestionBatch(){
        try {
            InputStream stream = getClass().getResourceAsStream("/data/work_portfolio.csv");
            List<Document> documents = parser.parseDocuments(stream);
            int n = documents.size();
            int end;
            for (int i = 0; i < n; i += 50) {
                end = Math.min(n, i + 50);
                List<Document> batch = documents.subList(i, end);

                List<String> texts = batch.stream().map(Document::content).toList();

                List<List<Float>> embeddings = embedBatch(texts, "search_document");

                List<Point> points = new ArrayList<>();

                for (int j = 0; j < embeddings.size(); j++) {
                    Document document = batch.get(j);
                    List<Float> embedding = embeddings.get(j);
                    Map<String, Object> payload = Map.of("content", document.content(), "title", document.title());
                    points.add(new Point(document.id(), embedding, payload));
                }

                qdrantVectorStore.upsertPoints(points);

                Thread.sleep(100000);

            }

        }catch (Exception e){
            throw new RuntimeException("Ingestion failed", e);
        }
    }
}
