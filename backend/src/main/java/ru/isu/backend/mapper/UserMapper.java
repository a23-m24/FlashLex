package ru.isu.backend.mapper;

import org.springframework.stereotype.Component;
import ru.isu.backend.dto.response.UserResponse;
import ru.isu.backend.model.User;
import ru.isu.backend.model.UserRole;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole() == null ? UserRole.STUDENT : user.getRole(),
                user.getDailyNewLimit(),
                user.getDailyReviewLimit(),
                Boolean.TRUE.equals(user.getPublicationBanned()),
                user.getRegisteredAt()
        );
    }
}
