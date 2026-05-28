package ru.isu.backend.mapper;

import org.springframework.stereotype.Component;
import ru.isu.backend.dto.response.UserResponse;
import ru.isu.backend.model.User;

@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getDailyNewLimit(),
                user.getDailyReviewLimit(),
                user.getRegisteredAt()
        );
    }
}
