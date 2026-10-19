package com.ticketing.ticket;

/** Result of scanning a ticket. "ticket" is filled in whenever the code exists. */
public record RedeemResult(boolean ok, String reason, Ticket ticket) {
}
