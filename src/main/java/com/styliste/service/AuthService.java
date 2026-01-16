package com.styliste.service;

import com.styliste.dto.AuthRequest;
import com.styliste.dto.AuthResponse;
import com.styliste.dto.SignUpRequest;
import com.styliste.entity.User;
import com.styliste.entity.UserRole;
import com.styliste.exception.ResourceAlreadyExistsException;
import com.styliste.repository.AppointmentRepository;
import com.styliste.repository.UserRepository;
import com.styliste.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentService appointmentService;

    @Value("${jwt.secret}")
    private String debugSecretKey;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public AuthResponse login(AuthRequest request) {
        // 1. Check entry
        System.out.println("========== KEY VERIFICATION ==========");

        // 1. Print the Key length (Safe-ish way to check)
        System.out.println("Key Length loaded: " + (debugSecretKey != null ? debugSecretKey.length() : "NULL"));

        // 2. Print the actual key (DANGEROUS - DELETE AFTER CHECKING)
        System.out.println("Key Value loaded: " + debugSecretKey);

        System.out.println("======================================");
        System.out.println("========== LOGIN DEBUG START ==========");
        System.out.println("1. Attempting login for email: " + request.getEmail());
        // Don't print the password for security, even in debug!

        try {
            // 2. Check Authentication Manager
            System.out.println("2. Calling AuthenticationManager...");

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            System.out.println("3. Authentication Manager Success! User is verified.");

            // 3. Check Token Generation
            System.out.println("4. Generating JWT Token...");
            String jwt = tokenProvider.generateToken(authentication);
            System.out.println("5. Token Generated Successfully: " + jwt.substring(0, 10) + "..."); // Print just the start

            // 4. Check DB Retrieval
            System.out.println("6. Fetching full user details from DB...");
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

            System.out.println("7. User found: " + user.getName() + " (ID: " + user.getId() + ")");
            System.out.println("========== LOGIN DEBUG END (SUCCESS) ==========");
            appointmentService.linkAppointmentsToUser(user);


            return AuthResponse.builder()
                    .token(jwt)
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole().name())
                    .message("Login successful")
                    .build();

        } catch (Exception ex) {
            // 5. Catch Failures
            System.out.println("========== LOGIN DEBUG END (FAILED) ==========");
            System.out.println("!!! ERROR OCCURRED !!!");
            System.out.println("Error Class: " + ex.getClass().getName());
            System.out.println("Error Message: " + ex.getMessage());

            // This helps you see if it's a Password error or a Key error
            ex.printStackTrace();

            log.error("Login failed: {}", ex.getMessage());
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public AuthResponse signup(SignUpRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResourceAlreadyExistsException("User already exists with this email");
        }

        // Create new user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(UserRole.CUSTOMER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);


        // Generate JWT token
        String jwt = tokenProvider.generateTokenFromUsername(savedUser.getEmail());

        log.info("User registered successfully: {}", savedUser.getEmail());
        appointmentService.linkAppointmentsToUser(savedUser);


        return AuthResponse.builder()
                .token(jwt)
                .id(savedUser.getId())
                .email(savedUser.getEmail())
                .name(savedUser.getName())
                .role(savedUser.getRole().name())
                .message("Signup successful")
                .build();
    }
}
