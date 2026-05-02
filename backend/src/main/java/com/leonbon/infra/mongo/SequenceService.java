package com.leonbon.infra.mongo;

import jakarta.annotation.PostConstruct;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Numeración estable para jugadores (ID legible; independiente del ObjectId de Mongo).
 */
@Service
public class SequenceService {
    static final long USER_LEON_INITIAL = 10000L;

    private final MongoOperations mongoOperations;

    public SequenceService(MongoOperations mongoOperations) {
        this.mongoOperations = mongoOperations;
    }

    @PostConstruct
    void ensureUserSequenceSeeded() {
        String id = NumericSequence.USER_LEON_NUMBER;
        if (!mongoOperations.exists(Query.query(Criteria.where("_id").is(id)), NumericSequence.class)) {
            mongoOperations.insert(new NumericSequence(id, USER_LEON_INITIAL));
        }
    }

    public long nextUserLeonPlayerNumber() {
        NumericSequence bumped = mongoOperations.findAndModify(
                Query.query(Criteria.where("_id").is(NumericSequence.USER_LEON_NUMBER)),
                new Update().inc("latest", 1),
                FindAndModifyOptions.options().returnNew(true).upsert(false),
                NumericSequence.class);
        if (bumped == null) {
            throw new IllegalStateException("unable to bump user sequence");
        }
        return bumped.getLatest();
    }
}
