package com.styliste.controller;

import com.styliste.dto.AddressDTO;
import com.styliste.dto.UserDTO;
import com.styliste.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserDTO>> getAllUsers(
            @RequestParam(required = false) String role, // 👈 Capture the role from URL
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {

        log.info("Fetching users with Role: {}, Page: {}, Size: {}", role, page, pageSize);
        return ResponseEntity.ok(userService.getAllUsers(role, page, pageSize));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateProfile(@PathVariable Long id, @RequestBody UserDTO dto) {
        return ResponseEntity.ok(userService.updateProfile(id, dto));
    }

    @PutMapping("/{id}/addresses/")

    @PostMapping("/{id}/change-password")
    public ResponseEntity<String> changePassword(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        userService.updatePassword(id, payload.get("oldPassword"), payload.get("newPassword"));
        return ResponseEntity.ok("Password updated successfully");
    }

    @GetMapping("/{id}/addresses")
    public ResponseEntity<List<AddressDTO>> getAddresses(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserAddresses(id));
    }

    // 5. Add New Address
    @PostMapping("/{id}/addresses")
    public ResponseEntity<AddressDTO> addAddress(@PathVariable Long id, @RequestBody AddressDTO dto) {
        return ResponseEntity.ok(userService.addAddress(id, dto));
    }

    // 6. Set Default Address
    @PatchMapping("/{id}/addresses/{addressId}/default")
    public ResponseEntity<Void> setDefault(@PathVariable Long id, @PathVariable Long addressId) {
        userService.setDefaultAddress(id, addressId);
        return ResponseEntity.noContent().build();
    }

    // 7. Update Existing Address
    @PutMapping("/{id}/addresses/{addrId}")
    public ResponseEntity<AddressDTO> updateAddress(
            @PathVariable Long id,
            @PathVariable Long addrId,
            @RequestBody AddressDTO dto
    ) {
        return ResponseEntity.ok(
                userService.updateAddress(id, addrId, dto)
        );
    }

    // 8. Delete Address
    @DeleteMapping("/{id}/addresses/{addrId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            @PathVariable Long addrId
    ) {
        userService.deleteAddress(id, addrId);
        return ResponseEntity.noContent().build();
    }



    // Endpoint to ACTIVATE a user
    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> activateUser(@PathVariable Long id) {
        UserDTO updatedUser = userService.updateUserStatus(id, true);
        return ResponseEntity.ok(updatedUser);
    }

    // Endpoint to DEACTIVATE a user
    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> deactivateUser(@PathVariable Long id) {
        UserDTO updatedUser = userService.updateUserStatus(id, false);
        return ResponseEntity.ok(updatedUser);
    }

    // Endpoint to PERMANENTLY DELETE a user
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully.");
    }
}