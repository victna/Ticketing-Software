package com.ticketing.ticket;

import java.time.Instant;

/** A single-use ticket. The name is already decrypted here. */
public record Ticket(
        String code,
        String event,
        String name,
        int year,
        Instant purchaseDate,
        boolean used,
        Instant usedAt) {
}
