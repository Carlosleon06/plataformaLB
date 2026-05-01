package com.leonbon.economy;

import com.leonbon.auth.ConflictException;
import com.leonbon.users.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class EconomyService {
    private final MongoTemplate mongoTemplate;
    private final TransactionRepository transactionRepository;
    private final EconomyConfig config;

    public EconomyService(MongoTemplate mongoTemplate, TransactionRepository transactionRepository, EconomyConfig config) {
        this.mongoTemplate = mongoTemplate;
        this.transactionRepository = transactionRepository;
        this.config = config;
    }

    public Transaction claimDaily(String userId) {
        Instant now = Instant.now();
        Instant windowStart = dailyWindowStart(now, config.zoneId(), config.dailyResetHour());

        Query q = new Query(new Criteria().andOperator(
                Criteria.where("_id").is(userId),
                new Criteria().orOperator(
                        Criteria.where("lastDailyClaimAt").exists(false),
                        Criteria.where("lastDailyClaimAt").lt(windowStart)
                )
        ));

        Update u = new Update()
                .set("lastDailyClaimAt", now)
                .inc("leonCoinsBalance", config.dailyClaimAmount())
                .set("updatedAt", now);

        User updated = mongoTemplate.findAndModify(
                q,
                u,
                FindAndModifyOptions.options().returnNew(true),
                User.class
        );

        if (updated == null) {
            throw new ConflictException("daily claim not available yet");
        }

        Transaction t = new Transaction();
        t.setUserId(userId);
        t.setType(TransactionType.DAILY_CLAIM);
        t.setAmount(config.dailyClaimAmount());
        t.setBalanceAfter(updated.getLeonCoinsBalance());
        t.setCreatedAt(now);
        return transactionRepository.save(t);
    }

    static Instant dailyWindowStart(Instant now, ZoneId zoneId, int resetHour) {
        LocalDate today = LocalDateTime.ofInstant(now, zoneId).toLocalDate();
        LocalDateTime todayReset = today.atTime(resetHour, 0);
        LocalDateTime localNow = LocalDateTime.ofInstant(now, zoneId);
        LocalDateTime windowStartLocal = localNow.isBefore(todayReset) ? todayReset.minusDays(1) : todayReset;
        return windowStartLocal.atZone(zoneId).toInstant();
    }
}

