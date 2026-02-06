package me.matteogiordano.consumer;

import java.time.LocalDateTime;

public record Message(
    String id,
    String content,
    LocalDateTime timestamp,
    String enrichedBy
) {}

