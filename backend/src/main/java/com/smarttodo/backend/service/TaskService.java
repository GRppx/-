package com.smarttodo.backend.service;

import com.smarttodo.backend.entity.Task;
import com.smarttodo.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {
    
    @Autowired
    private TaskRepository taskRepository;
    
    public List<Task> getTasksByUserId(Long userId) {
        return taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }
    
    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("任务不存在"));
    }
    
    public Task createTask(Task task) {
        if (task.getTitle() == null || task.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("任务名称不能为空");
        }
        return taskRepository.save(task);
    }
    
    public Task updateTask(Long id, Task task) {
        Task existing = taskRepository.findById(id).orElseThrow();
        if (task.getTitle() != null && !task.getTitle().trim().isEmpty()) {
            existing.setTitle(task.getTitle());
        }
        if (task.getDescription() != null) {
            existing.setDescription(task.getDescription());
        }
        if (task.getPriority() != null) {
            existing.setPriority(task.getPriority());
        }
        if (task.getDeadline() != null) {
            existing.setDeadline(task.getDeadline());
        }
        if (task.getStatus() != null) {
            existing.setStatus(task.getStatus());
        }

        if (task.getStatus() == Task.TaskStatus.IN_PROGRESS) {
            if (task.getStartedAt() != null) {
                existing.setStartedAt(task.getStartedAt());
            } else if (existing.getStartedAt() == null) {
                existing.setStartedAt(LocalDateTime.now());
            }
            existing.setCompletedAt(null);
        } else if (task.getStatus() == Task.TaskStatus.COMPLETED) {
            if (existing.getCompletedAt() == null) {
                existing.setCompletedAt(LocalDateTime.now());
            }
            existing.setStartedAt(null);
        } else if (task.getStatus() != null) {
            existing.setStartedAt(null);
            existing.setCompletedAt(null);
        }

        existing.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(existing);
    }
    
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }
    
    public Task addTime(Long id, Long seconds) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.setTotalSeconds(task.getTotalSeconds() + seconds);
        task.setStartedAt(null);
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    public Task completeTask(Long id, Long seconds) {
        Task task = taskRepository.findById(id).orElseThrow();
        task.setTotalSeconds(task.getTotalSeconds() + seconds);
        task.setStartedAt(null);
        task.setStatus(Task.TaskStatus.COMPLETED);
        if (task.getCompletedAt() == null) {
            task.setCompletedAt(LocalDateTime.now());
        }
        task.setUpdatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }
}