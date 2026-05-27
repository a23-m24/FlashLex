package ru.isu.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.isu.backend.model.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
