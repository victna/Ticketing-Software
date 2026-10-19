package com.ticketing.ticket;

import com.ticketing.security.EncryptionService;
import com.ticketing.util.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Optional;

/** Java version of ticket.js: create, scan, list, and the yearly wipe. */
@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final JdbcTemplate jdbc;
    private final EncryptionService crypto;
    private final CodeGenerator codes;
    private final String dataDir;

    private final RowMapper<Ticket> mapper;

    public TicketService(JdbcTemplate jdbc,
                         EncryptionService crypto,
                         CodeGenerator codes,
                         @Value("${ticketing.data-dir:data}") String dataDir) {
        this.jdbc = jdbc;
        this.crypto = crypto;
        this.codes = codes;
        this.dataDir = dataDir;
        this.mapper = (rs, rowNum) -> {
            String code = rs.getString("code");
            String usedAt = rs.getString("used_at");
            return new Ticket(
                    code,
                    rs.getString("event"),
                    crypto.decryptText(rs.getString("name"), nameContext(code)),
                    rs.getInt("year"),
                    Instant.parse(rs.getString("purchase_date")),
                    rs.getInt("used") == 1,
                    usedAt == null ? null : Instant.parse(usedAt));
        };
    }

    private static String nameContext(String code) {
        return "ticket:name:" + code;
    }

    // ---------- Yearly wipe ----------

    /** Runs once when the server has started. */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        checkYearRollover();
    }

    /** The server stays on, so also check shortly after midnight every day. */
    @Scheduled(cron = "0 1 0 * * *")
    public void scheduledCheck() {
        checkYearRollover();
    }

    /**
     * If the calendar year has changed since the last check, saves a backup
     * copy of the database and deletes every ticket. Passes are not touched.
     */
    public synchronized void checkYearRollover() {
        int currentYear = Year.now().getValue();
        List<String> rows = jdbc.queryForList(
                "SELECT value FROM meta WHERE key = 'lastCleanupYear'", String.class);

        if (rows.isEmpty()) {
            // First run ever: just remember the year
            jdbc.update("INSERT INTO meta (key, value) VALUES ('lastCleanupYear', ?)",
                    String.valueOf(currentYear));
            return;
        }

        int lastYear = Integer.parseInt(rows.get(0));
        if (currentYear > lastYear) {
            Path backup = backupDatabase(lastYear);        // if this fails, nothing is deleted
            int deleted = jdbc.update("DELETE FROM tickets");
            jdbc.update("UPDATE meta SET value = ? WHERE key = 'lastCleanupYear'",
                    String.valueOf(currentYear));
            log.info("New year detected: {} tickets deleted. Backup saved at {}", deleted, backup);
        }
    }

    private Path backupDatabase(int year) {
        try {
            Path dir = Path.of(dataDir, "backups").toAbsolutePath();
            Files.createDirectories(dir);
            Path file = dir.resolve("ticketing-backup-" + year + ".db");
            Files.deleteIfExists(file);
            // VACUUM INTO writes a consistent copy of the live SQLite database
            jdbc.execute("VACUUM INTO '" + file.toString().replace("'", "''") + "'");
            return file;
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the yearly backup", e);
        }
    }

    // ---------- Tickets ----------

    public Ticket createTicket(String event, String name, Integer year) {
        if (event == null || event.isBlank()) {
            throw new IllegalArgumentException("An event name is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A name is required");
        }
        int ticketYear = (year == null) ? Year.now().getValue() : year;
        if (ticketYear < 2000 || ticketYear > 2100) {
            throw new IllegalArgumentException("That is not a valid year");
        }

        checkYearRollover();
        String purchaseDate = Instant.now().toString();

        while (true) {
            String code = codes.generate("TKT");
            String encryptedName = crypto.encryptText(name.trim(), nameContext(code));
            int inserted = jdbc.update(
                    "INSERT OR IGNORE INTO tickets (code, event, name, year, purchase_date) VALUES (?, ?, ?, ?, ?)",
                    code, event.trim(), encryptedName, ticketYear, purchaseDate);
            if (inserted == 1) {
                return getTicket(code).orElseThrow();
            }
            // The code already existed (extremely unlikely), so try a new one
        }
    }

    /** Called when a code is scanned at the door. */
    public RedeemResult redeemTicket(String code) {
        checkYearRollover();
        String normalized = CodeGenerator.normalize(code);

        // One UPDATE both checks and marks the ticket, so two scans can't both succeed
        int changed = jdbc.update(
                "UPDATE tickets SET used = 1, used_at = ? WHERE code = ? AND used = 0",
                Instant.now().toString(), normalized);

        Optional<Ticket> ticket = getTicket(normalized);
        if (ticket.isEmpty()) {
            return new RedeemResult(false, "Invalid ticket", null);
        }
        if (changed == 0) {
            return new RedeemResult(false, "Already used", ticket.get());
        }
        return new RedeemResult(true, null, ticket.get());
    }

    public Optional<Ticket> getTicket(String code) {
        List<Ticket> found = jdbc.query("SELECT * FROM tickets WHERE code = ?", mapper,
                CodeGenerator.normalize(code));
        return found.stream().findFirst();
    }

    public List<Ticket> listTickets() {
        checkYearRollover();
        return jdbc.query("SELECT * FROM tickets ORDER BY purchase_date", mapper);
    }

    /** WARNING: permanently deletes every ticket. Returns how many were removed. */
    public int deleteAllTickets() {
        return jdbc.update("DELETE FROM tickets");
    }
}
