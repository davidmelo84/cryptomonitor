// back/src/main/java/com/crypto/repository/TransactionRepository.java

package com.crypto.repository;

import com.crypto.model.Transaction;
import com.crypto.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByUserOrderByTransactionDateDesc(User user);
    List<Transaction> findByUserAndCoinSymbolOrderByTransactionDateDesc(User user, String coinSymbol);
}