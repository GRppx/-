package com.smarttodo.backend.controller;

import com.smarttodo.backend.entity.User;
import com.smarttodo.backend.repository.UserRepository;
import com.smarttodo.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User newUser = userService.register(user);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "注册成功");
            response.put("data", newUser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        try {
            User user = userService.login(username, password);
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "登录成功");
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("username", user.getUsername());
            data.put("nickname", user.getNickname());
            data.put("role", user.getRole().name());
            response.put("data", data);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 400);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/check/{username}")
    public ResponseEntity<?> checkUsername(@PathVariable String username) {
        boolean exists = userService.existsByUsername(username);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers(@RequestParam Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("无效的请求用户"));
        if (requester.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("仅管理员可获取用户列表");
        }
        List<Map<String, Object>> users = userRepository.findAll().stream().map(user -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", user.getId());
            item.put("username", user.getUsername());
            item.put("nickname", user.getNickname());
            item.put("email", user.getEmail());
            item.put("role", user.getRole().name());
            item.put("createdAt", user.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("data", users);
        return ResponseEntity.ok(response);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                        @RequestParam Long requesterId) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new IllegalArgumentException("无效的请求用户"));
        if (requester.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("仅管理员可删除用户");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        userRepository.delete(user);
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "删除成功");
        return ResponseEntity.ok(response);
    }
}