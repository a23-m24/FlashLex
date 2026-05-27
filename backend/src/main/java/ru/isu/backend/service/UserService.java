package ru.isu.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.isu.backend.dto.request.GoalSettingsRequest;
import ru.isu.backend.dto.request.ProfileRequest;
import ru.isu.backend.dto.response.UserResponse;
import ru.isu.backend.exception.DuplicateResourceException;
import ru.isu.backend.exception.NotFoundException;
import ru.isu.backend.mapper.UserMapper;
import ru.isu.backend.model.User;
import ru.isu.backend.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserResponse getUser(Long userId) {
        return userMapper.toResponse(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Long userId, ProfileRequest request) {
        User user = findUser(userId);
        String email = request.email().trim().toLowerCase();
        userRepository.findByEmail(email)
                .filter(found -> !found.getId().equals(userId))
                .ifPresent(found -> {
                    throw new DuplicateResourceException("Email is already registered");
                });

        user.setName(request.name().trim());
        user.setEmail(email);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateGoals(Long userId, GoalSettingsRequest request) {
        User user = findUser(userId);
        user.setDailyNewLimit(request.dailyNewLimit());
        user.setDailyReviewLimit(request.dailyReviewLimit());
        return userMapper.toResponse(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
