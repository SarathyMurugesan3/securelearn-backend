package com.example.demo.admin.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.user.model.User;
import com.example.demo.user.repository.UserRepository;

@RestController
@RequestMapping("/api/admin/manage-users")
public class AdminUserManagementController {
	
	private final UserRepository userRepository;
	
	@Autowired
	public AdminUserManagementController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}
	
	@GetMapping
	public Page<User> getUsers(@RequestParam(defaultValue="0") int page,
			@RequestParam(defaultValue="10") int size,
			@RequestParam(defaultValue="id") String sortBy){
		Pageable pageable = PageRequest.of(page, size,Sort.by(sortBy));
		return userRepository.findAll(pageable);
	}
	
	@PostMapping("/{id}/block")
	public String blockUser(@PathVariable String id) {
		User user = userRepository.findById(id).orElseThrow();
		user.setBlocked(true);
		userRepository.save(user);
		return "User blocked";
	}
	
	@PostMapping("/{id}/unblock")
	public String unblockUser(@PathVariable String id) {
		User user = userRepository.findById(id).orElseThrow();
		user.setBlocked(false);
		user.setRiskScore(0);
		userRepository.save(user);
		
		return "User unblocked";
	}
	
}




