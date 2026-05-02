package com.leonbon.infra.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "numeric_sequences")
public class NumericSequence {
    public static final String USER_LEON_NUMBER = "user_leon_player_number";

    @Id
    private String id;

    private long latest;

    public NumericSequence() {}

    public NumericSequence(String id, long latest) {
        this.id = id;
        this.latest = latest;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getLatest() {
        return latest;
    }

    public void setLatest(long latest) {
        this.latest = latest;
    }
}
