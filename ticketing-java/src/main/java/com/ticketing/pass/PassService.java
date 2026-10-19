package com.ticketing.pass;

import com.ticketing.photo.PhotoService;
import com.ticketing.security.EncryptionService;
import com.ticketing.util.CodeGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Java version of seasonPass.js: create passes, gate scans, and limited perks. */
@Service
public class PassService {

    public static final int CURRENT_SEASON = 2027;

    private final JdbcTemplate jdbc;
    private final EncryptionService crypto;
    private final CodeGenerator codes;
    private final PhotoService photos;

    private final RowMapper<Pass> mapper;

    public PassService(JdbcTemplate jdbc, EncryptionService crypto, CodeGenerator codes, PhotoService photos) {
        this.jdbc = jdbc;
        this.crypto = crypto;
        this.codes = codes;
        this.photos = photos;
        this.mapper = (rs, rowNum) -> {
            String code = rs.getString("code");
            return new Pass(
                    code,
                    PassType.valueOf(rs.getString("type")),
                    crypto.decryptText(rs.getString("name"), nameContext(code)),
                    rs.getInt("season"),
                    Instant.parse(rs.getString("purchase_date")),
                    LocalDate.parse(rs.getString("valid_through")));
        };
    }

    private static String nameContext(String code) {
        return "pass:name:" + code;
    }

    // ---------- Creating and looking up passes ----------

    /** type is "explorer", "hero", or "legend". */
    public Pass createPass(String type, String name, Integer season) {
        PassType passType = PassType.fromName(type);
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A name is required");
        }
        int passSeason = (season == null) ? CURRENT_SEASON : season;
        String purchaseDate = Instant.now().toString();

        while (true) {
            String code = codes.generate("PASS");
            String encryptedName = crypto.encryptText(name.trim(), nameContext(code));
            int inserted = jdbc.update("""
                            INSERT OR IGNORE INTO passes (code, type, name, season, purchase_date, valid_through)
                            VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    code, passType.name(), encryptedName, passSeason, purchaseDate,
                    passType.validThrough().toString());
            if (inserted == 1) {
                return getPass(code).orElseThrow();
            }
            // The code already existed (extremely unlikely), so try a new one
        }
    }

    public Optional<Pass> getPass(String code) {
        List<Pass> found = jdbc.query("SELECT * FROM passes WHERE code = ?", mapper,
                CodeGenerator.normalize(code));
        return found.stream().findFirst();
    }

    public List<Pass> listPasses() {
        return jdbc.query("SELECT * FROM passes ORDER BY purchase_date", mapper);
    }

    public List<Visit> listVisits(String code) {
        return jdbc.query(
                "SELECT id, park, visited_at FROM visits WHERE code = ? ORDER BY visited_at",
                (rs, i) -> new Visit(rs.getLong("id"), rs.getString("park"),
                        Instant.parse(rs.getString("visited_at"))),
                CodeGenerator.normalize(code));
    }

    // ---------- Gate scans and perks ----------

    /** Gate scan. Passes are reusable, so every valid scan is logged as a visit. */
    public synchronized PassResult checkIn(String code, String park) {
        Optional<Pass> found = getPass(code);
        if (found.isEmpty()) {
            return new PassResult(false, "Invalid pass", null, null, false);
        }
        Pass pass = found.get();

        if (LocalDate.now().isAfter(pass.validThrough())) {
            return reject("Pass expired", pass);
        }
        if (park == null || park.isBlank()) {
            return reject("A park name is required", pass);
        }
        if (!pass.type().validAt(park)) {
            return reject("Not valid at " + park.trim(), pass);
        }

        jdbc.update("INSERT INTO visits (code, park, visited_at) VALUES (?, ?, ?)",
                pass.code(), park.trim(), Instant.now().toString());

        return accept(pass);
    }

    /** Uses one limited perk: fastLane, freeFriendTicket, or friendDiscount. */
    public synchronized PassResult usePerk(String code, String perkName) {
        Optional<Pass> found = getPass(code);
        if (found.isEmpty()) {
            return new PassResult(false, "Invalid pass", null, null, false);
        }
        Pass pass = found.get();

        if (LocalDate.now().isAfter(pass.validThrough())) {
            return reject("Pass expired", pass);
        }

        Perk perk;
        try {
            perk = Perk.fromName(perkName);
        } catch (IllegalArgumentException e) {
            return reject(e.getMessage(), pass);
        }

        int limit = pass.type().limit(perk);
        if (limit == 0) {
            return reject("Not included with the " + pass.type().label() + " pass", pass);
        }
        if (usesSoFar(pass.code(), perk) >= limit) {
            return reject("No " + perk.apiName() + " uses left this "
                    + perk.period().name().toLowerCase(), pass);
        }

        jdbc.update("INSERT INTO perk_uses (code, perk, used_at) VALUES (?, ?, ?)",
                pass.code(), perk.apiName(), Instant.now().toString());

        return accept(pass);
    }

    // ---------- Helpers ----------

    private PassResult reject(String reason, Pass pass) {
        return new PassResult(false, reason, pass, null, photos.hasPhoto(pass.code()));
    }

    private PassResult accept(Pass pass) {
        return new PassResult(true, null, pass, describeBenefits(pass), photos.hasPhoto(pass.code()));
    }

    private int usesSoFar(String code, Perk perk) {
        Integer count;
        if (perk.period() == Perk.Period.MONTH) {
            String month = YearMonth.now(ZoneOffset.UTC).toString();   // like 2026-10, matches stored UTC times
            count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM perk_uses WHERE code = ? AND perk = ? AND substr(used_at, 1, 7) = ?",
                    Integer.class, code, perk.apiName(), month);
        } else {
            count = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM perk_uses WHERE code = ? AND perk = ?",
                    Integer.class, code, perk.apiName());
        }
        return count == null ? 0 : count;
    }

    /** Everything the pass includes, plus how many perk uses are left. */
    public Benefits describeBenefits(Pass pass) {
        PassType type = pass.type();

        Map<String, Integer> left = new LinkedHashMap<>();
        for (Perk perk : Perk.values()) {
            left.put(perk.apiName(), Math.max(0, type.limit(perk) - usesSoFar(pass.code(), perk)));
        }

        return new Benefits(
                type.allParks() ? "All 8 parks" : String.join(", ", type.parks()),
                type.foodDiscount() + "%",
                type.retailDiscount() + "%",
                type.freeGeneralParking(),
                type.preferredParkingDiscount() + "%",
                type.frightFestYears().stream().map(String::valueOf).collect(Collectors.joining(" & ")),
                type.combo10Pack(),
                left);
    }
}
