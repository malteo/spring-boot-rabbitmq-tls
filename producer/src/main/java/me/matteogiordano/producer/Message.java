package me.matteogiordano.producer;

import java.time.LocalDateTime;

public record Message(
    String id,
    String content,
    LocalDateTime timestamp,
    String enrichedBy
) {}

