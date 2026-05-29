package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.isu.backend.dto.request.LoginRequest;
import ru.isu.backend.dto.request.RegisterRequest;
import ru.isu.backend.dto.response.AuthResponse;
import ru.isu.backend.exception.DuplicateResourceException;
import ru.isu.backend.mapper.UserMapper;
import ru.isu.backend.model.User;
import ru.isu.backend.repository.UserRepository;
import ru.isu.backend.security.JwtService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email уже используется");
        }
        String name = request.name().trim();
        if (userRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Имя уже занято");
        }

        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDailyNewLimit(request.dailyNewLimit());
        user.setDailyReviewLimit(request.dailyReviewLimit());

        User saved = userRepository.save(user);
        return toAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(token, "Bearer", userMapper.toResponse(user));
    }
}
