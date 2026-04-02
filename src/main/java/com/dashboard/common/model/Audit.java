package com.dashboard.common.model;

import lombok.Data;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.Instant;

@Data
public class Audit {
    private Instant createdAt = Instant.now();
    private Instant updatedAt;
    @Indexed
    private Instant deletedAt;
}
