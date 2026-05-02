package com.leonbon.notifications;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserNotificationRepository extends MongoRepository<UserNotification, String> {
    List<UserNotification> findByUserIdAndCreatedAtAfterOrderByCreatedAtDesc(String userId, Instant after, Pageable page);
}
