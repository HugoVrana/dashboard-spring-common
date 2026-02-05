package com.dashboard.common.model;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ActivityEvent {
    private String id;
    private String type; // USER_REGISTERED, VALUES_EDITED, etc.
    private String actorId;
    private String actorName;
    private LocalDateTime timestamp;
    private Map<String, Object> metadata;
}