package com.ticketing.pass;

import java.time.LocalDate;
import java.util.List;

/**
 * The three season pass types, from the 2027 pass pages.
 * To change a price, discount, or park list, edit it here and nowhere else.
 *
 * Check these against the official terms. The Hero and Legend end dates were not
 * visible, so 2027-12-31 is a placeholder.
 */
public enum PassType {

    EXPLORER("Explorer", 55.99,
            List.of("Mid America Adventure", "Mid America Water Park"),
            LocalDate.of(2027, 9, 6),
            0, 0, false, 0,
            List.of(2026), false,
            0, 0, 0),

    HERO("Hero", 75.99,
            List.of("Mid America Adventure", "Mid America Water Park", "Worlds of Fun"),
            LocalDate.of(2027, 12, 31),
            10, 10, true, 0,
            List.of(2026, 2027), true,
            0, 0, 1),

    LEGEND("Legend", 130.99,
            List.of(),                                   // empty list = all 8 parks
            LocalDate.of(2027, 12, 31),
            15, 15, true, 50,
            List.of(2026, 2027), true,
            4, 2, 2);

    private final String label;
    private final double price;
    private final List<String> parks;
    private final LocalDate validThrough;
    private final int foodDiscount;
    private final int retailDiscount;
    private final boolean freeGeneralParking;
    private final int preferredParkingDiscount;
    private final List<Integer> frightFestYears;
    private final boolean combo10Pack;
    private final int fastLaneLimit;
    private final int freeFriendTicketLimit;
    private final int friendDiscountLimit;

    PassType(String label, double price, List<String> parks, LocalDate validThrough,
             int foodDiscount, int retailDiscount, boolean freeGeneralParking,
             int preferredParkingDiscount, List<Integer> frightFestYears, boolean combo10Pack,
             int fastLaneLimit, int freeFriendTicketLimit, int friendDiscountLimit) {
        this.label = label;
        this.price = price;
        this.parks = parks;
        this.validThrough = validThrough;
        this.foodDiscount = foodDiscount;
        this.retailDiscount = retailDiscount;
        this.freeGeneralParking = freeGeneralParking;
        this.preferredParkingDiscount = preferredParkingDiscount;
        this.frightFestYears = frightFestYears;
        this.combo10Pack = combo10Pack;
        this.fastLaneLimit = fastLaneLimit;
        this.freeFriendTicketLimit = freeFriendTicketLimit;
        this.friendDiscountLimit = friendDiscountLimit;
    }

    public String label() { return label; }
    public double price() { return price; }
    public List<String> parks() { return parks; }
    public LocalDate validThrough() { return validThrough; }
    public int foodDiscount() { return foodDiscount; }
    public int retailDiscount() { return retailDiscount; }
    public boolean freeGeneralParking() { return freeGeneralParking; }
    public int preferredParkingDiscount() { return preferredParkingDiscount; }
    public List<Integer> frightFestYears() { return frightFestYears; }
    public boolean combo10Pack() { return combo10Pack; }

    public boolean allParks() {
        return parks.isEmpty();
    }

    /** Is this pass accepted at the given park? (not case sensitive) */
    public boolean validAt(String park) {
        if (allParks()) {
            return true;
        }
        String wanted = park == null ? "" : park.trim();
        return parks.stream().anyMatch(p -> p.equalsIgnoreCase(wanted));
    }

    /** How many times a perk may be used (per season or per month, see Perk.period). */
    public int limit(Perk perk) {
        return switch (perk) {
            case FAST_LANE -> fastLaneLimit;
            case FREE_FRIEND_TICKET -> freeFriendTicketLimit;
            case FRIEND_DISCOUNT -> friendDiscountLimit;
        };
    }

    /** Accepts "legend", "Legend", or "LEGEND". */
    public static PassType fromName(String text) {
        String wanted = text == null ? "" : text.trim();
        for (PassType type : values()) {
            if (type.name().equalsIgnoreCase(wanted)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown pass type \"" + wanted + "\". Use: explorer, hero, legend");
    }
}
