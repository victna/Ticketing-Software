package com.ticketing.pass;

import java.util.Map;

/** What a pass includes, plus how many limited perk uses are left. */
public record Benefits(
        String parks,
        String foodDiscount,
        String retailDiscount,
        boolean freeGeneralParking,
        String preferredParkingDiscount,
        String frightFestYears,
        boolean combo10Pack,
        Map<String, Integer> perksLeft) {
}
