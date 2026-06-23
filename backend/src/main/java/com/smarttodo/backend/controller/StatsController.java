package com.smarttodo.backend.controller;

import com.smarttodo.backend.entity.Task;
import com.smarttodo.backend.entity.User;
import com.smarttodo.backend.repository.TaskRepository;
import com.smarttodo.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {
    
    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getStats(@PathVariable Long userId,
                                      @RequestParam(required = false) Long requesterId) {
        if (requesterId != null && !requesterId.equals(userId)) {
            User requester = userRepository.findById(requesterId)
                    .orElseThrow(() -> new IllegalArgumentException("无效的请求用户"));
            if (requester.getRole() != User.Role.ADMIN) {
                throw new IllegalArgumentException("无权限查看该用户的统计数据");
            }
        }
        List<Task> allTasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
        
        long total = allTasks.size();
        long pending = allTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.PENDING).count();
        long inProgress = allTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.IN_PROGRESS).count();
        long completed = allTasks.stream().filter(t -> t.getStatus() == Task.TaskStatus.COMPLETED).count();
        long totalSeconds = allTasks.stream().mapToLong(t -> {
            long base = t.getTotalSeconds() != null ? t.getTotalSeconds() : 0L;
            if (t.getStatus() == Task.TaskStatus.IN_PROGRESS && t.getStartedAt() != null) {
                long extra = Duration.between(t.getStartedAt(), LocalDateTime.now()).getSeconds();
                return base + Math.max(0, extra);
            }
            return base;
        }).sum();
        
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("pending", pending);
        data.put("inProgress", inProgress);
        data.put("completed", completed);
        data.put("totalSeconds", totalSeconds);
        data.put("completionRate", total > 0 ? Math.round(completed * 100.0 / total) : 0);
        
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}