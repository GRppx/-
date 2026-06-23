package com.smarttodo.backend.controller;

import com.smarttodo.backend.entity.Task;
import com.smarttodo.backend.entity.User;
import com.smarttodo.backend.repository.UserRepository;
import com.smarttodo.backend.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/backend/tasks")
@CrossOrigin(origins = "*")
public class BackendController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    // 兼容路径： /api/backend/tasks/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTasks(@PathVariable Long userId) {
        List<Task> tasks = taskService.getTasksByUserId(userId);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", tasks);
        return ResponseEntity.ok(response);
    }

    // 兼容查看所有任务（管理员可用）
    @GetMapping("/all")
    public ResponseEntity<?> getAllTasks(@RequestParam(required = false) Long requesterId) {
        if (requesterId == null) {
            // 返回所有任务，但不包含 ownerName（直接用于调试）
            List<Map<String, Object>> allTasks = taskService.getAllTasks().stream().map(task -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", task.getId());
                item.put("title", task.getTitle());
                item.put("userId", task.getUserId());
                item.put("status", task.getStatus());
                item.put("priority", task.getPriority());
                return item;
            }).collect(Collectors.toList());
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("data", allTasks);
            return ResponseEntity.ok(response);
        }
        User requester = userRepository.findById(requesterId).orElseThrow(() -> new IllegalArgumentException("无效的请求用户"));
        if (requester.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("仅管理员可查看全部任务");
        }
        List<Map<String, Object>> allTasks = taskService.getAllTasks().stream().map(task -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", task.getId());
            item.put("title", task.getTitle());
            item.put("description", task.getDescription());
            item.put("status", task.getStatus());
            item.put("priority", task.getPriority());
            item.put("deadline", task.getDeadline());
            item.put("userId", task.getUserId());
            item.put("ownerName", userRepository.findById(task.getUserId()).map(User::getUsername).orElse("未知用户"));
            item.put("totalSeconds", task.getTotalSeconds());
            item.put("startedAt", task.getStartedAt());
            item.put("createdAt", task.getCreatedAt());
            item.put("updatedAt", task.getUpdatedAt());
            return item;
        }).collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", allTasks);
        return ResponseEntity.ok(response);
    }
}
