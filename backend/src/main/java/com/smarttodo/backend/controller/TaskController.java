package com.smarttodo.backend.controller;

import com.smarttodo.backend.entity.Task;
import com.smarttodo.backend.entity.User;
import com.smarttodo.backend.repository.UserRepository;
import com.smarttodo.backend.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {
    
    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;
    
    private User getRequester(Long requesterId) {
        return userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("无效的请求用户"));
    }

    private boolean isAdmin(Long requesterId) {
        return getRequester(requesterId).getRole() == User.Role.ADMIN;
    }

    private boolean isOwner(Long requesterId, Task task) {
        return task.getUserId() != null && task.getUserId().equals(requesterId);
    }

    private void assertTaskAccess(Task task, Long requesterId) {
        if (task.getUserId() == null) {
            throw new IllegalArgumentException("任务所属用户信息异常");
        }
        if (!task.getUserId().equals(requesterId) && !isAdmin(requesterId)) {
            throw new IllegalArgumentException("无权限操作该任务");
        }
    }

    private void assertTaskModificationAllowed(Task task, Long requesterId, Task request) {
        if (isAdmin(requesterId)) {
            return;
        }
        if (task.getAdminCreated() != null && task.getAdminCreated() && isOwner(requesterId, task)) {
            if (request != null && request.getStatus() == Task.TaskStatus.COMPLETED && task.getStatus() == Task.TaskStatus.IN_PROGRESS) {
                return;
            }
            throw new IllegalArgumentException("无权限修改该任务");
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTasks(@PathVariable Long userId,
                                      @RequestParam(required = false) Long requesterId) {
        if (requesterId == null) requesterId = userId;
        if (!requesterId.equals(userId) && !isAdmin(requesterId)) {
            throw new IllegalArgumentException("无权限查看该用户的任务");
        }
        List<Task> tasks = taskService.getTasksByUserId(userId);
        List<Map<String, Object>> data = tasks.stream().map(task -> {
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
            item.put("completedAt", task.getCompletedAt());
            item.put("adminCreated", task.getAdminCreated());
            item.put("createdAt", task.getCreatedAt());
            item.put("updatedAt", task.getUpdatedAt());
            return item;
        }).collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllTasks(@RequestParam(required = false) Long requesterId) {
        if (requesterId == null) {
            throw new IllegalArgumentException("请求用户不能为空");
        }
        if (!isAdmin(requesterId)) {
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
            item.put("completedAt", task.getCompletedAt());
            item.put("adminCreated", task.getAdminCreated());
            item.put("createdAt", task.getCreatedAt());
            item.put("updatedAt", task.getUpdatedAt());
            return item;
        }).collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", allTasks);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Task task,
                                        @RequestParam(required = false) Long requesterId) {
        if (requesterId == null) {
            throw new IllegalArgumentException("请求用户不能为空");
        }
        if (!isAdmin(requesterId) || task.getUserId() == null || task.getUserId() <= 0L) {
            task.setUserId(requesterId);
        }
        if (isAdmin(requesterId) && task.getUserId() != null && !task.getUserId().equals(requesterId)) {
            task.setStatus(Task.TaskStatus.IN_PROGRESS);
            task.setStartedAt(LocalDateTime.now());
            task.setCompletedAt(null);
            task.setTotalSeconds(0L);
            task.setAdminCreated(true);
        }
        Task created = taskService.createTask(task);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", created);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id,
                                        @RequestBody Task task,
                                        @RequestParam(required = false) Long requesterId) {
        if (requesterId == null) {
            throw new IllegalArgumentException("请求用户不能为空");
        }
        Task existing = taskService.getTaskById(id);
        assertTaskAccess(existing, requesterId);
        assertTaskModificationAllowed(existing, requesterId, task);
        Task updated = taskService.updateTask(id, task);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", updated);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id,
                                        @RequestParam(required = false) Long requesterId) {
        if (requesterId == null) {
            throw new IllegalArgumentException("请求用户不能为空");
        }
        Task existing = taskService.getTaskById(id);
        assertTaskAccess(existing, requesterId);
        if (existing.getAdminCreated() != null && existing.getAdminCreated() && !isAdmin(requesterId)) {
            throw new IllegalArgumentException("无权限删除该任务");
        }
        taskService.deleteTask(id);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/time")
    public ResponseEntity<?> addTime(@PathVariable Long id,
                                     @RequestBody Map<String, Long> body,
                                     @RequestParam(required = false) Long requesterId) {
        if (requesterId == null) {
            throw new IllegalArgumentException("请求用户不能为空");
        }
        Task existing = taskService.getTaskById(id);
        assertTaskAccess(existing, requesterId);
        if (existing.getAdminCreated() != null && existing.getAdminCreated() && !isAdmin(requesterId)) {
            throw new IllegalArgumentException("请通过完成接口结束管理员指派任务");
        }
        Task task = taskService.addTime(id, body.get("seconds"));
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", task);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeTask(@PathVariable Long id,
                                          @RequestBody Map<String, Long> body,
                                          @RequestParam(required = false) Long requesterId) {
        if (requesterId == null) {
            throw new IllegalArgumentException("请求用户不能为空");
        }
        Task existing = taskService.getTaskById(id);
        assertTaskAccess(existing, requesterId);
        if (!isAdmin(requesterId) && existing.getAdminCreated() != null && existing.getAdminCreated() && !isOwner(requesterId, existing)) {
            throw new IllegalArgumentException("无权限完成该任务");
        }
        Task task = taskService.completeTask(id, body.get("seconds"));
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", task);
        return ResponseEntity.ok(response);
    }
}
