package com.aman.uptime.service;

import com.aman.uptime.dto.AuthResponse;
import com.aman.uptime.dto.LoginRequest;
import com.aman.uptime.dto.RegisterRequest;
import com.aman.uptime.entity.Role;
import com.aman.uptime.entity.User;
import com.aman.uptime.exception.BadRequestException;
import com.aman.uptime.exception.ResourceNotFoundException;
import com.aman.uptime.exception.UnauthorizedException;
import com.aman.uptime.repository.UserRepository;
import com.aman.uptime.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already registered");
        }
        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        // Sabse pehla registered user (tum) ADMIN banta hai, baaki USER
        user.setRole(userRepository.count() == 0 ? Role.ADMIN : Role.USER);
        userRepository.save(user);
        return toResponse(user, jwtService.generateToken(user.getEmail(), user.getRole().name()));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return toResponse(user, jwtService.generateToken(user.getEmail(), user.getRole().name()));
    }

    public AuthResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toResponse(user, null);
    }

    private AuthResponse toResponse(User user, String token) {
        return new AuthResponse(token, user.getName(), user.getEmail(), user.getRole().name());
    }
}
