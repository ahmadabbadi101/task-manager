package com.abady.taskmanager.service;

import com.abady.taskmanager.entity.Task;
import com.abady.taskmanager.exception.EntityNotFoundException;
import com.abady.taskmanager.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    public Task createTask(Task task) {
        return taskRepository.save(task);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Task", id));
    }

    public Task updateTask(Long id, Task updated) {
        Task existing = getTaskById(id);
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setDueDate(updated.getDueDate());
        existing.setPriority(updated.getPriority());
        existing.setStatus(updated.getStatus());
        return taskRepository.save(existing);
    }

    public void deleteTask(Long id) {
        getTaskById(id);
        taskRepository.deleteById(id);
    }
}
