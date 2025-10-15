package com.crypto.repository;

import com.crypto.dto.CryptoCurrency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CryptoCurrencyRepository extends JpaRepository<CryptoCurrency, Long> {

    Optional<CryptoCurrency> findByCoinId(String coinId);

    Optional<CryptoCurrency> findBySymbol(String symbol);

    List<CryptoCurrency> findAllByOrderByMarketCapDesc();

    @Query("SELECT c FROM CryptoCurrency c WHERE c.priceChange24h > :threshold")
    List<CryptoCurrency> findCryptosWithPriceIncrease(Double threshold);

    @Query("SELECT c FROM CryptoCurrency c WHERE c.priceChange24h < :threshold")
    List<CryptoCurrency> findCryptosWithPriceDecrease(Double threshold);
}