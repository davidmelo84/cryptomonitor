package com.crypto.repository;

import com.crypto.model.Portfolio;
import com.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    // Consultas padrão
    List<Portfolio> findByUser(User user);

    List<Portfolio> findByUserOrderByTotalInvestedDesc(User user);

    Optional<Portfolio> findByUserAndCoinSymbol(User user, String coinSymbol);

    // =========================================
    // Projeção otimizada para não carregar User completo
    // =========================================
    @Query("SELECT p.id as id, p.coinSymbol as coinSymbol, p.coinName as coinName, " +
            "p.quantity as quantity, p.averageBuyPrice as averageBuyPrice, " +
            "p.totalInvested as totalInvested " +
            "FROM Portfolio p WHERE p.user.username = :username " +
            "ORDER BY p.totalInvested DESC")
    List<PortfolioProjection> findByUserUsernameOptimized(String username);

    interface PortfolioProjection {
        Long getId();
        String getCoinSymbol();
        String getCoinName();
        BigDecimal getQuantity();
        BigDecimal getAverageBuyPrice();
        BigDecimal getTotalInvested();
    }
}
