package com.styliste.service;

import com.styliste.dto.AddressDTO;
import com.styliste.dto.UserDTO;
import com.styliste.entity.Address;
import com.styliste.entity.Appointment;
import com.styliste.entity.User;
import com.styliste.entity.UserRole;
import com.styliste.exception.BadRequestException;
import com.styliste.exception.ResourceNotFoundException;
import com.styliste.repository.AddressRepository;
import com.styliste.repository.AppointmentRepository;
import com.styliste.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;
@Autowired
private AddressRepository addressRepository;
    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private  PasswordEncoder passwordEncoder;

    // Inside UserService.java

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return mapToDTO(user);
    }

    public UserDTO updateProfile(Long id, UserDTO dto) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        // Note: Email usually isn't changed here for security; use a separate flow if needed.
        return mapToDTO(userRepository.save(user));
    }

    public void updatePassword(Long id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("Current password does not match");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public List<AddressDTO> getUserAddresses(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getAddresses().stream().map(this::mapAddressToDTO).toList();
    }

    public AddressDTO addAddress(Long userId, AddressDTO dto) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // If this is the first address or set as default, handle existing defaults
        if (dto.getIsDefault() || user.getAddresses().isEmpty()) {
            resetDefaultAddresses(user);
        }

        Address address = Address.builder()
                .user(user)
                .addressLine1(dto.getAddressLine1())
                .addressLine2(dto.getAddressLine2())
                .city(dto.getCity())
                .state(dto.getState())
                .postalCode(dto.getPostalCode())
                .country(dto.getCountry())
                .contactPhone(dto.getContactPhone()) // 👈 Map from request
                .isDefault(user.getAddresses().isEmpty() ? true : dto.getIsDefault())
                .build();

        return mapAddressToDTO(addressRepository.save(address));
    }

    public void setDefaultAddress(Long userId, Long addressId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        resetDefaultAddresses(user);

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    private void resetDefaultAddresses(User user) {
        user.getAddresses().forEach(a -> a.setIsDefault(false));
        addressRepository.saveAll(user.getAddresses());
    }

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

    private AddressDTO mapAddressToDTO(Address a) {
        return AddressDTO.builder()
                .id(a.getId())
                .addressLine1(a.getAddressLine1())
                .addressLine2(a.getAddressLine2())
                .city(a.getCity())
                .state(a.getState())
                .contactPhone(a.getContactPhone()) // 👈 Map to response
                .postalCode(a.getPostalCode())
                .country(a.getCountry())
                .isDefault(a.getIsDefault())
                .build();
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