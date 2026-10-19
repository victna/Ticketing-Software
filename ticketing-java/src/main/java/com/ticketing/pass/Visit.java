package com.ticketing.pass;

import java.time.Instant;

/** One logged gate scan. */
public record Visit(long id, String park, Instant visitedAt) {
}
