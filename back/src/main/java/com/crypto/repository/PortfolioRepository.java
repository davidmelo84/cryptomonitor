package com.crypto.repository;

import com.crypto.model.Portfolio;
import com.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {
    List<Portfolio> findByUser(User user);
    List<Portfolio> findByUserOrderByTotalInvestedDesc(User user);
    Optional<Portfolio> findByUserAndCoinSymbol(User user, String coinSymbol);
}