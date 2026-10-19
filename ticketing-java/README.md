# Ticketing (Java / Spring Boot / SQLite)

Java version of the JavaScript ticketing project: single-use tickets, reusable season passes
(Explorer, Hero, Legend), pass photos taken on an Android phone or tablet, a yearly ticket
wipe, and encrypted storage for holder names and photos.

## What replaced what

| JavaScript file | Java |
|---|---|
| `ticket.js` | `ticket/TicketService.java` (+ `Ticket`, `RedeemResult`) |
| `seasonPass.js` | `pass/PassService.java`, `PassType.java`, `Perk.java` (+ result records) |
| `server.js` | `photo/PhotoController.java`, `photo/PhotoService.java` |
| `create.js`, `scan.js`, `createPass.js`, `scanPass.js` | `TicketController`, `PassController` and the page `static/index.html` |
| `better-sqlite3` + `tickets.db` / `passes.db` | SQLite through JDBC, one file: `data/ticketing.db` |

## Set up (Windows + VS Code)

1. Install a **JDK 21** (for example Eclipse Temurin 21) and **Apache Maven** (maven.apache.org/download.cgi).
   Open a new terminal and check: `java -version` and `mvn -v`.
2. In VS Code install the **Extension Pack for Java**.
3. **File -> Open Folder** and choose this `ticketing-java` folder.
4. In the terminal, in this folder:
   ```
   mvn spring-boot:run
   ```
   The first run downloads libraries, so it takes a few minutes.
5. Open `http://localhost:8080` in a browser.

The database is created by itself the first time: `data/ticketing.db`.
To start fresh, stop the server and delete the `data` folder (this also deletes the key, so any
old database can no longer be read).

To run the checks that don't need the server: `mvn test`.

## Phone / tablet photos

1. Start the server. The console prints addresses like `http://192.168.1.25:8080`.
2. Create a pass on the web page. It shows a `photoPage` address.
3. Open that address in Chrome on the Android device (same Wi-Fi), tap **Take photo**, then **Save photo**.
4. If Windows asks about the firewall, allow **private networks**.

## Encryption

* Holder names and photos are encrypted with **AES-256-GCM** before they reach the database.
  Each value has its own random IV and is tied to its record, so it can't be copied onto another one.
* Ticket and pass **codes** are not encrypted. They are random and used for lookups.
* The key: if the environment variable `TICKETING_ENCRYPTION_KEY` is set (Base64, 32 bytes) it is used.
  Otherwise the app creates `data/secret.key` on first start. Fine for development, but a key next to the
  database protects little if someone copies the whole folder. For real use, set the environment variable
  and keep the key somewhere safe. **If the key is lost, the encrypted data cannot be recovered.**
  PowerShell command to make a key:
  ```
  $b = New-Object byte[] 32; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); [Convert]::ToBase64String($b)
  ```
* Not done yet: HTTPS for traffic between devices and the server, and staff logins. Until then anyone on
  your Wi-Fi who knows a code can use the pages, so only run this on a network you trust.

## Endpoints

| Method and path | What it does |
|---|---|
| `POST /api/tickets` | Create a ticket: `{"event":"...","name":"...","year":2026}` (year is optional) |
| `POST /api/tickets/{code}/redeem` | Scan a ticket (works once) |
| `GET /api/tickets` | List tickets |
| `DELETE /api/tickets?confirm=DELETE-ALL` | Delete every ticket |
| `POST /api/passes` | Create a pass: `{"type":"legend","name":"..."}` |
| `POST /api/passes/{code}/checkin?park=Worlds of Fun` | Gate scan, logs a visit |
| `POST /api/passes/{code}/perks/fastLane` | Use a perk (`fastLane`, `freeFriendTicket`, `friendDiscount`) |
| `GET /api/passes`, `GET /api/passes/{code}/visits` | Lists |
| `GET /photo/{code}`, `GET /view/{code}` | Take a photo / view the pass photo |

## Yearly wipe

When the calendar year changes (checked on startup, on every ticket action, and daily at 00:01) the app
saves a backup copy of the database in `data/backups/` and then deletes all tickets. Season passes are
never deleted by this. The backup holds encrypted names and photos, so keep it as private as the database.

## Before you trust the numbers

Prices, park lists, discounts, and end dates are in `PassType.java`. They were read from screenshots, so check them
against the official terms. The Hero and Legend end dates are placeholders (2027-12-31).

## Git

`.gitignore` already excludes `data/`, `*.db`, and `secret.key`. Never commit those.
