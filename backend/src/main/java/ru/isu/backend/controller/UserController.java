package ru.isu.backend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.isu.backend.dto.request.GoalSettingsRequest;
import ru.isu.backend.dto.request.ProfileRequest;
import ru.isu.backend.dto.response.UserResponse;
import ru.isu.backend.security.UserPrincipal;
import ru.isu.backend.service.UserService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
public class UserController {

    private final UserService userService;

    @GetMapping
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.getUser(principal.getId());
    }

    @PutMapping
    public UserResponse updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProfileRequest request
    ) {
        return userService.updateProfile(principal.getId(), request);
    }

    @PutMapping("/goals")
    public UserResponse updateGoals(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GoalSettingsRequest request
    ) {
        return userService.updateGoals(principal.getId(), request);
    }
}
