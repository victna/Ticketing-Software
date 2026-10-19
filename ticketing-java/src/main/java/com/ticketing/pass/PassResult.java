package com.ticketing.pass;

/**
 * Result of a gate scan or a perk use.
 * "pass" is filled in whenever the code exists; "benefits" only when ok is true.
 */
public record PassResult(boolean ok, String reason, Pass pass, Benefits benefits, boolean photoOnFile) {
}
