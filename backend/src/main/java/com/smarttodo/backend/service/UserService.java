package com.smarttodo.backend.service;

import com.smarttodo.backend.entity.User;
import com.smarttodo.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User register(User user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已被注册");
        }
        user.setRole(User.Role.USER);
        return userRepository.save(user);
    }
    
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户名不存在"));
        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("密码错误");
        }
        return user;
    }
    
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}