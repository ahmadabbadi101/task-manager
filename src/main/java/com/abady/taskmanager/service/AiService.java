package com.abady.taskmanager.service;

import com.abady.taskmanager.dto.BreakdownResponse;
import com.abady.taskmanager.entity.Task;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${ai.api.key}")
    private String apiKey;

    @Value("${ai.api.url}")
    private String apiUrl;

    @Value("${ai.model}")
    private String model;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BreakdownResponse breakdown(Task task) {
        String prompt = buildPrompt(task);
        String rawResponse = callApi(prompt);
        List<String> subtasks = parseSubtasks(rawResponse);
        return new BreakdownResponse(task.getId(), task.getTitle(), subtasks);
    }

    private String buildPrompt(Task task) {
        return """
                Break down the following task into clear, actionable subtasks.

                Title: %s
                Description: %s
                Priority: %s
                Due date: %s

                Respond with JSON only, using this exact structure:
                {"subtasks": ["subtask 1", "subtask 2", "subtask 3"]}
                """.formatted(
                task.getTitle(),
                task.getDescription() != null ? task.getDescription() : "N/A",
                task.getPriority(),
                task.getDueDate() != null ? task.getDueDate().toString() : "N/A"
        );
    }

    private String callApi(String prompt) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                        Map.of("role", "system",
                               "content", "You are a task breakdown assistant. Always respond with valid JSON only. No markdown, no explanation, no code blocks."),
                        Map.of("role", "user", "content", prompt)
                )
        );

        return restClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);
    }

    private List<String> parseSubtasks(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode subtasksNode = objectMapper.readTree(content).path("subtasks");
            List<String> subtasks = new ArrayList<>();
            subtasksNode.forEach(node -> subtasks.add(node.asText()));
            return subtasks;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage(), e);
        }
    }
}
