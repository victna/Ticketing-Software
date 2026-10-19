package com.ticketing.pass;

import java.time.Instant;
import java.time.LocalDate;

/** A season pass. The name is already decrypted here. */
public record Pass(
        String code,
        PassType type,
        String name,
        int season,
        Instant purchaseDate,
        LocalDate validThrough) {
}
