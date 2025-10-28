package com.crypto.repository;

import com.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email); // ✅ NOVO
    boolean existsByEmail(String email); // ✅ NOVO
}
