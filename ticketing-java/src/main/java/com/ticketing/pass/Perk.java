package com.ticketing.pass;

/** Perks with a limited number of uses. */
public enum Perk {
    FAST_LANE("fastLane", Period.SEASON),
    FREE_FRIEND_TICKET("freeFriendTicket", Period.SEASON),
    FRIEND_DISCOUNT("friendDiscount", Period.MONTH);   // 20% off friend tickets

    /** How often the limit resets: once per season, or once per calendar month. */
    public enum Period { SEASON, MONTH }

    private final String apiName;
    private final Period period;

    Perk(String apiName, Period period) {
        this.apiName = apiName;
        this.period = period;
    }

    public String apiName() {
        return apiName;
    }

    public Period period() {
        return period;
    }

    /** Accepts "fastLane" or "FAST_LANE", in any letter case. */
    public static Perk fromName(String text) {
        String wanted = text == null ? "" : text.trim();
        for (Perk perk : values()) {
            if (perk.apiName.equalsIgnoreCase(wanted) || perk.name().equalsIgnoreCase(wanted)) {
                return perk;
            }
        }
        throw new IllegalArgumentException("Unknown perk \"" + wanted
                + "\". Use: fastLane, freeFriendTicket, friendDiscount");
    }
}
