package com.crypto.repository;

import com.crypto.model.VerificationToken;
import com.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);
    Optional<VerificationToken> findByCode(String code);
    Optional<VerificationToken> findByUser(User user);
    void deleteByUser(User user);
}
