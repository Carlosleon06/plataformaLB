package com.leonbon.economy;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    Optional<Transaction> findByRef(String ref);

    List<Transaction> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
}

