package com.abady.taskmanager;

import com.abady.taskmanager.entity.Task;
import com.abady.taskmanager.enums.Priority;
import com.abady.taskmanager.enums.Status;
import com.abady.taskmanager.exception.EntityNotFoundException;
import com.abady.taskmanager.repository.TaskRepository;
import com.abady.taskmanager.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask() {
        return Task.builder()
                .title("Write unit tests")
                .description("Cover all service methods")
                .dueDate(LocalDate.of(2026, 7, 1))
                .priority(Priority.HIGH)
                .status(Status.TODO)
                .build();
    }

    // ── createTask ───────────────────────────────────────────────────────────

    @Test
    void createTask_savesAndReturnsTask() {
        Task input = sampleTask();
        Task saved = sampleTask();
        when(taskRepository.save(input)).thenReturn(saved);

        Task result = taskService.createTask(input);

        assertThat(result).isEqualTo(saved);
        verify(taskRepository).save(input);
    }

    // ── getAllTasks ──────────────────────────────────────────────────────────

    @Test
    void getAllTasks_returnsAllTasks() {
        List<Task> tasks = List.of(sampleTask(), sampleTask());
        when(taskRepository.findAll()).thenReturn(tasks);

        List<Task> result = taskService.getAllTasks();

        assertThat(result).hasSize(2);
        verify(taskRepository).findAll();
    }

    // ── getTaskById ──────────────────────────────────────────────────────────

    @Test
    void getTaskById_returnsTask_whenExists() {
        Task task = sampleTask();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Task result = taskService.getTaskById(1L);

        assertThat(result.getTitle()).isEqualTo("Write unit tests");
    }

    @Test
    void getTaskById_throwsEntityNotFoundException_whenMissing() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── updateTask ───────────────────────────────────────────────────────────

    @Test
    void updateTask_updatesFieldsAndReturnsTask() {
        Task existing = sampleTask();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task update = Task.builder()
                .title("Updated title")
                .description("New description")
                .dueDate(LocalDate.of(2026, 8, 1))
                .priority(Priority.MEDIUM)
                .status(Status.IN_PROGRESS)
                .build();

        Task result = taskService.updateTask(1L, update);

        assertThat(result.getTitle()).isEqualTo("Updated title");
        assertThat(result.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(result.getPriority()).isEqualTo(Priority.MEDIUM);
        verify(taskRepository).save(existing);
    }

    // ── deleteTask ───────────────────────────────────────────────────────────

    @Test
    void deleteTask_deletesTask_whenExists() {
        Task task = sampleTask();
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.deleteTask(1L);

        verify(taskRepository).deleteById(1L);
    }
}
