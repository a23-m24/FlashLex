package ru.isu.backend.security;

import ru.isu.backend.model.User;

public record CurrentUser(Long id, String email) {

    public static CurrentUser from(User user) {
        return new CurrentUser(user.getId(), user.getEmail());
    }
}
