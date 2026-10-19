const crypto = require('crypto');
const fs = require('fs');
const Database = require('better-sqlite3');

const DB_FILE = 'tickets.db';
const db = new Database(DB_FILE);

db.exec(`
  CREATE TABLE IF NOT EXISTS tickets (
    code         TEXT PRIMARY KEY,
    event        TEXT NOT NULL,
    name         TEXT NOT NULL,
    year         INTEGER NOT NULL,
    purchaseDate TEXT NOT NULL,
    used         INTEGER NOT NULL DEFAULT 0,
    usedAt       TEXT
  );
  CREATE TABLE IF NOT EXISTS meta (
    key   TEXT PRIMARY KEY,
    value TEXT NOT NULL
  );
`);

// ---------- Yearly wipe ----------

// If the calendar year has changed since the last check, back up and
// delete every ticket. Called automatically before every operation below.
function checkYearRollover() {
    const currentYear = new Date().getFullYear();
    const row = db.prepare("SELECT value FROM meta WHERE key = 'lastCleanupYear'").get();

    if (!row) {
        // First run ever: just record the year, nothing to delete
        db.prepare("INSERT INTO meta (key, value) VALUES ('lastCleanupYear', ?)").run(String(currentYear));
        return;
    }

    if (currentYear > Number(row.value)) {
        // Safety net: keep a copy of the old data before wiping
        fs.copyFileSync(DB_FILE, `tickets-backup-${row.value}.db`);
        db.prepare('DELETE FROM tickets').run();
        db.prepare("UPDATE meta SET value = ? WHERE key = 'lastCleanupYear'").run(String(currentYear));
        console.log(`New year detected: all tickets deleted (backup saved as tickets-backup-${row.value}.db)`);
    }
}

// ---------- Codes ----------

const ALPHABET = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';

function generateCode() {
    const bytes = crypto.randomBytes(12);
    let chars = '';
    for (let i = 0; i < 12; i++) {
        chars += ALPHABET[bytes[i] % ALPHABET.length];
    }
    return `TKT-${chars.slice(0, 4)}-${chars.slice(4, 8)}-${chars.slice(8, 12)}`;
}

// ---------- Tickets ----------

const insertTicket = db.prepare(`
  INSERT INTO tickets (code, event, name, year, purchaseDate)
  VALUES (?, ?, ?, ?, ?)
`);

// year defaults to the current year; purchase date is set to right now
function createTicket(eventName, name, year = new Date().getFullYear()) {
    checkYearRollover();
    const purchaseDate = new Date().toISOString();

    while (true) {
        const code = generateCode();
        try {
            insertTicket.run(code, eventName, name, year, purchaseDate);
            return db.prepare('SELECT * FROM tickets WHERE code = ?').get(code);
        } catch (err) {
            if (err.code !== 'SQLITE_CONSTRAINT_PRIMARYKEY') throw err;
        }
    }
}

const markUsed = db.prepare(
    'UPDATE tickets SET used = 1, usedAt = ? WHERE code = ? AND used = 0'
);

// Called when a code is scanned. Always returns the ticket details
// (name, year, purchase date) when the code exists.
function redeemTicket(code) {
    checkYearRollover();
    code = code.trim().toUpperCase();

    const result = markUsed.run(new Date().toISOString(), code);
    const ticket = db.prepare('SELECT * FROM tickets WHERE code = ?').get(code);

    if (!ticket) return { ok: false, reason: 'Invalid ticket' };
    if (result.changes === 0) return { ok: false, reason: 'Already used', ticket };
    return { ok: true, ticket };
}

function listTickets() {
    checkYearRollover();
    return db.prepare('SELECT * FROM tickets').all();
}

// WARNING: permanently deletes every ticket
function deleteAllTickets() {
    return db.prepare('DELETE FROM tickets').run().changes;
}

checkYearRollover();   // also runs once whenever the program starts

module.exports = { generateCode, createTicket, redeemTicket, listTickets, deleteAllTickets };