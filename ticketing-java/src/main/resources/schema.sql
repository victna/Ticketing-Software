-- Tickets: single-use, wiped at the start of each new calendar year.
-- "name" holds the AES-256-GCM encrypted holder name (Base64 text).
CREATE TABLE IF NOT EXISTS tickets (
  code          TEXT PRIMARY KEY,
  event         TEXT NOT NULL,
  name          TEXT NOT NULL,
  year          INTEGER NOT NULL,
  purchase_date TEXT NOT NULL,
  used          INTEGER NOT NULL DEFAULT 0,
  used_at       TEXT
);

-- Small key/value table (remembers which year the last cleanup ran)
CREATE TABLE IF NOT EXISTS meta (
  key   TEXT PRIMARY KEY,
  value TEXT NOT NULL
);

-- Season passes: reusable, never wiped by the yearly ticket cleanup.
-- "name" is encrypted, same as tickets.
CREATE TABLE IF NOT EXISTS passes (
  code          TEXT PRIMARY KEY,
  type          TEXT NOT NULL,
  name          TEXT NOT NULL,
  season        INTEGER NOT NULL,
  purchase_date TEXT NOT NULL,
  valid_through TEXT NOT NULL
);

-- One row per successful gate scan
CREATE TABLE IF NOT EXISTS visits (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  code       TEXT NOT NULL,
  park       TEXT NOT NULL,
  visited_at TEXT NOT NULL
);

-- One row per perk used (fast lane, free friend ticket, friend discount)
CREATE TABLE IF NOT EXISTS perk_uses (
  id      INTEGER PRIMARY KEY AUTOINCREMENT,
  code    TEXT NOT NULL,
  perk    TEXT NOT NULL,
  used_at TEXT NOT NULL
);

-- Pass photos: "image" holds the encrypted bytes
CREATE TABLE IF NOT EXISTS photos (
  code      TEXT PRIMARY KEY,
  mime_type TEXT NOT NULL,
  image     BLOB NOT NULL,
  taken_at  TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_visits_code ON visits (code);
CREATE INDEX IF NOT EXISTS idx_perk_uses_code ON perk_uses (code, perk);
