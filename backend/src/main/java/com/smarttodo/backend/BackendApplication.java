package com.smarttodo.backend;

import com.smarttodo.backend.entity.User;
import com.smarttodo.backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner initAdmin(UserRepository userRepository) {
		return args -> {
			// 默认管理员账号（如需改回 admin/admin123，可修改这里）
			if (!userRepository.existsByUsername("23300208")) {
				User admin = new User();
				admin.setUsername("23300208");
				admin.setPassword("23300208");
				admin.setEmail("admin@smarttodo.local");
				admin.setNickname("管理员");
				admin.setRole(User.Role.ADMIN);
				userRepository.save(admin);
			}
		};
	}
}
