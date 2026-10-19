package com.example.bankapp.repository;

import com.example.bankapp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountId(Long accountId);

        @Query("""
                        SELECT transaction FROM Transaction transaction
                        WHERE transaction.account.id = :accountId
                            AND (:searchTerm IS NULL OR LOWER(transaction.type) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
                            AND (:transactionType = 'ALL'
                                     OR (:transactionType = 'DEPOSIT' AND LOWER(transaction.type) LIKE '%deposit%')
                                     OR (:transactionType = 'WITHDRAWAL' AND LOWER(transaction.type) LIKE '%withdrawal%')
                                     OR (:transactionType = 'TRANSFER' AND LOWER(transaction.type) LIKE '%transfer%'))
                            AND (:fromDate IS NULL OR transaction.timestamp >= :fromDate)
                            AND (:toDateExclusive IS NULL OR transaction.timestamp < :toDateExclusive)
                        ORDER BY transaction.timestamp DESC, transaction.id DESC
                        """)
        List<Transaction> findFilteredByAccountId(@Param("accountId") Long accountId,
                                                                                             @Param("searchTerm") String searchTerm,
                                                                                             @Param("transactionType") String transactionType,
                                                                                             @Param("fromDate") LocalDateTime fromDate,
                                                                                             @Param("toDateExclusive") LocalDateTime toDateExclusive);
}
