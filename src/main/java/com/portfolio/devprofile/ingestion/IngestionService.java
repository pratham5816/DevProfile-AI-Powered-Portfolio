package com.portfolio.devprofile.ingestion;

import com.portfolio.devprofile.dto.Document;
import com.portfolio.devprofile.embedding.CohereEmbeddingService;
import com.portfolio.devprofile.embedding.OllamaEmbeddingService;
import com.portfolio.devprofile.vectorstore.Point;
import com.portfolio.devprofile.vectorstore.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class IngestionService {

    private final Parser parser;
    private final OllamaEmbeddingService ollamaEmbeddingService;
    private final CohereEmbeddingService cohereEmbeddingService;
    private final QdrantVectorStore qdrantVectorStore;

    public IngestionService(@Autowired Parser parser,
                            @Autowired OllamaEmbeddingService ollamaEmbeddingService,
                            @Autowired CohereEmbeddingService cohereEmbeddingService,
                            @Autowired QdrantVectorStore qdrantVectorStore) {
        this.parser = parser;
        this.ollamaEmbeddingService = ollamaEmbeddingService;
        this.cohereEmbeddingService = cohereEmbeddingService;
        this.qdrantVectorStore = qdrantVectorStore;
    }

    public void runIngestion(){
        try{
            InputStream stream = getClass().getResourceAsStream("/data/work_portfolio.csv");
            List<Document> documents = parser.parseDocuments(stream);
            List<Point> points = new ArrayList<>();
            for(Document document : documents){
                Map<String, Object> payload = Map.of("content" , document.content() , "title" , document.title());
                List<Float> list = ollamaEmbeddingService.embed(document.content());
                points.add(new Point(document.id(), list , payload));
            }
            qdrantVectorStore.upsertPoints(points);
        }catch (Exception e){
            throw new RuntimeException(e);
        }

    }

    public void ingestResource(String resourcePath, String defaultTitle) {
        try (InputStream stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                System.err.println("Resource not found: " + resourcePath);
                return;
            }
            List<Document> documents = parser.parseMarkdown(stream, defaultTitle);
            if (documents.isEmpty()) {
                System.out.println("No documents parsed from: " + resourcePath);
                return;
            }
            List<Point> points = new ArrayList<>();
            for (Document document : documents) {
                Map<String, Object> payload = Map.of("content", document.content(), "title", document.title());
                List<Float> list = cohereEmbeddingService.embed(document.content(), "search_document");
                points.add(new Point(document.id(), list, payload));
            }
            qdrantVectorStore.upsertPoints(points);
            System.out.println("Successfully ingested " + points.size() + " points from " + resourcePath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to ingest " + resourcePath, e);
        }
    }

    public void checkDocuments(String resourcePath, String defaultTitle) {
        try(InputStream inputStream = getClass().getResourceAsStream(resourcePath)){
            if(inputStream == null) {
                System.err.println("Resource not found: " + resourcePath);
                return;
            }
            List<Document> documents = parser.parseMarkdown(inputStream, defaultTitle);
            for(Document document : documents){
                System.out.println("------------------------------------------------");
                System.out.println(String.format("Found document: %s", document.id()));
                System.out.println(String.format("Title: %s", document.title()));
                System.out.println(String.format("content: %s", document.content()));
                System.out.println("------------------------------------------------");
            }
        }catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public void ingestTest() {
        checkDocuments("/data/projects/education.md", "Education and Qualifications: Pratham");
        checkDocuments("/data/projects/devprofile-ai.md", "Project: DevProfile AI — RAG Portfolio Assistant");
        checkDocuments("/data/projects/vehicle-tracker.md", "Vehicle Tracker — Pratham Kumar Taheem: Browser-Based Fleet & Route Tracking System");
        checkDocuments("/data/projects/eatclub.md", "EatClub Automation — Pratham Kumar Taheem: Java Spring Boot, React, API Reverse Engineering & Cloud Architecture");
        checkDocuments("/data/projects/gchat-github-agent.md", "Portfolio Project: Real-Time Agentic RAG & GitHub–Google Chat Autonomous Assistant");
        checkDocuments("/data/projects/BLE-beacon.md", "BeaconBLE — Pratham Kumar Taheem: Smart Proximity Lighting with BLE & React Native");
        checkDocuments("/data/projects/langchain.md", "LangChain Learning Stack (RAG Pipeline Context)");
    }

    public void ingestMdFiles() {
        ingestResource("/data/projects/education.md", "Education and Qualifications: Pratham");
        ingestResource("/data/projects/devprofile-ai.md", "Project: DevProfile AI — RAG Portfolio Assistant");
        ingestResource("/data/projects/vehicle-tracker.md", "Vehicle Tracker — Pratham Kumar Taheem: Browser-Based Fleet & Route Tracking System");
        ingestResource("/data/projects/eatclub.md", "EatClub Automation — Pratham Kumar Taheem: Java Spring Boot, React, API Reverse Engineering & Cloud Architecture");
        ingestResource("/data/projects/gchat-github-agent.md", "Portfolio Project: Real-Time Agentic RAG & GitHub–Google Chat Autonomous Assistant");
        ingestResource("/data/projects/BLE-beacon.md", "BeaconBLE — Pratham Kumar Taheem: Smart Proximity Lighting with BLE & React Native");
        ingestResource("/data/projects/langchain.md", "LangChain Learning Stack (RAG Pipeline Context)");
    }
}
