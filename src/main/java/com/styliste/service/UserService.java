package com.styliste.service;

import com.styliste.dto.UserDTO;
import com.styliste.entity.User;
import com.styliste.entity.UserRole;
import com.styliste.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Inside UserService.java

    public Page<UserDTO> getAllUsers(String role, Integer page, Integer pageSize) { // 👈 Added 'role' param
        log.debug("Fetching users. Role filter: {}", role);

        int pageNum = (page != null) ? page : 0;
        int size = (pageSize != null) ? pageSize : 10;
        Pageable pageable = PageRequest.of(pageNum, size, Sort.by("id").descending());

        Page<User> userPage;

        // Check if a role was provided
        if (role != null && !role.isEmpty()) {
            try {
                // Convert String to Enum (e.g., "CUSTOMER" -> UserRole.CUSTOMER)
                UserRole userRole = UserRole.valueOf(role.toUpperCase());
                userPage = userRepository.findByRole(userRole, pageable);
            } catch (IllegalArgumentException e) {
                // If they send an invalid role (e.g., "SUPERMAN"), return empty list or throw error
                log.warn("Invalid role requested: {}", role);
                return Page.empty();
            }
        } else {
            // No role filter? Return everyone.
            userPage = userRepository.findAll(pageable);
        }

        return userPage.map(this::mapToDTO);
    }
    public UserDTO updateUserStatus(Long userId, boolean isActive) {
        // 1. Fetch User from DB
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // 2. Update status
        user.setIsActive(isActive);

        // 3. Save to DB
        User savedUser = userRepository.save(user);

        // 4. Convert Entity back to DTO
        return mapToDTO(savedUser);
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check agar user ke pass koi important data hai
        if (user.getOrders() != null && !user.getOrders().isEmpty()) {
            throw new RuntimeException("Cannot delete user. They have existing orders. Please deactivate instead.");
        } else if (user.getAppointments()!=null && !user.getAppointments().isEmpty()) {
            throw new RuntimeException("Cannot delete user. They have existing Appointments. Please deactivate instead.");
        }

        userRepository.delete(user);
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                // Safe checks for null lists to avoid NullPointerExceptions
                .orderCount(user.getOrders() != null ? user.getOrders().size() : 0)
                .appointmentCount(user.getAppointments() != null ? user.getAppointments().size() : 0)
                .build();
    }
}