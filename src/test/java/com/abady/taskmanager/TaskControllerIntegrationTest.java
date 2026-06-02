package com.abady.taskmanager;

import com.abady.taskmanager.dto.BreakdownResponse;
import com.abady.taskmanager.entity.Task;
import com.abady.taskmanager.enums.Priority;
import com.abady.taskmanager.enums.Status;
import com.abady.taskmanager.repository.TaskRepository;
import com.abady.taskmanager.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @MockBean
    private AiService aiService;

    @BeforeEach
    void clearDatabase() {
        taskRepository.deleteAll();
    }

    private Task savedTask() {
        return taskRepository.save(Task.builder()
                .title("Fix login bug")
                .description("OAuth token expires too fast")
                .dueDate(LocalDate.of(2026, 7, 1))
                .priority(Priority.HIGH)
                .status(Status.TODO)
                .build());
    }

    // ── POST /tasks ──────────────────────────────────────────────────────────

    @Test
    void createTask_returns201_withCreatedTask() throws Exception {
        Task task = Task.builder()
                .title("New task")
                .priority(Priority.LOW)
                .status(Status.TODO)
                .build();

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("New task"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createTask_returns400_whenTitleMissing() throws Exception {
        Task task = Task.builder()
                .priority(Priority.LOW)
                .status(Status.TODO)
                .build();

        mockMvc.perform(post("/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(task)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── GET /tasks ───────────────────────────────────────────────────────────

    @Test
    void getAllTasks_returns200_withTaskList() throws Exception {
        savedTask();

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Fix login bug"));
    }

    // ── GET /tasks/{id} ──────────────────────────────────────────────────────

    @Test
    void getTaskById_returns200_whenExists() throws Exception {
        Task task = savedTask();

        mockMvc.perform(get("/tasks/" + task.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(task.getId()))
                .andExpect(jsonPath("$.title").value("Fix login bug"));
    }

    @Test
    void getTaskById_returns404_whenMissing() throws Exception {
        mockMvc.perform(get("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── PUT /tasks/{id} ──────────────────────────────────────────────────────

    @Test
    void updateTask_returns200_withUpdatedFields() throws Exception {
        Task task = savedTask();

        Task update = Task.builder()
                .title("Fix login bug — updated")
                .description("OAuth token expires too fast")
                .dueDate(LocalDate.of(2026, 7, 1))
                .priority(Priority.HIGH)
                .status(Status.IN_PROGRESS)
                .build();

        mockMvc.perform(put("/tasks/" + task.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Fix login bug — updated"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    // ── DELETE /tasks/{id} ───────────────────────────────────────────────────

    @Test
    void deleteTask_returns204_andTaskIsGone() throws Exception {
        Task task = savedTask();

        mockMvc.perform(delete("/tasks/" + task.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/tasks/" + task.getId()))
                .andExpect(status().isNotFound());
    }

    // ── POST /tasks/{id}/breakdown ───────────────────────────────────────────

    @Test
    void breakdown_returns200_withExpectedShape() throws Exception {
        Task task = savedTask();

        BreakdownResponse mockResponse = new BreakdownResponse(
                task.getId(),
                task.getTitle(),
                List.of("Set up OAuth provider", "Implement token refresh", "Add expiry validation")
        );
        when(aiService.breakdown(any(Task.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/tasks/" + task.getId() + "/breakdown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(task.getId()))
                .andExpect(jsonPath("$.title").value("Fix login bug"))
                .andExpect(jsonPath("$.subtasks").isArray())
                .andExpect(jsonPath("$.subtasks.length()").value(3))
                .andExpect(jsonPath("$.subtasks[0]").value("Set up OAuth provider"));
    }
}
