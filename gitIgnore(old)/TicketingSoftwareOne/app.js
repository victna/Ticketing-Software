const { createTicket, redeemTicket, listTickets, deleteAllTickets } = require('./ticket.js');

// ---------- Formatting helpers ----------

// 2026-10-19T04:05:59.308Z  ->  Oct 19, 2026
function formatDate(iso) {
    return new Date(iso).toLocaleDateString('en-US', {
        year: 'numeric', month: 'short', day: 'numeric'
    });
}

// 2026-10-19T04:05:59.308Z  ->  Oct 19, 2026, 12:05 AM
function formatDateTime(iso) {
    return new Date(iso).toLocaleString('en-US', {
        year: 'numeric', month: 'short', day: 'numeric',
        hour: 'numeric', minute: '2-digit'
    });
}

// Prints one scan result as a small card
function showScan(result) {
    const line = '-'.repeat(40);
    console.log('\n' + line);
    console.log(result.ok ? '  VALID TICKET' : `  REJECTED: ${result.reason}`);
    console.log(line);

    if (result.ticket) {
        const t = result.ticket;
        console.log(`  Name:       ${t.name}`);
        console.log(`  Event:      ${t.event}`);
        console.log(`  Year:       ${t.year}`);
        console.log(`  Purchased:  ${formatDate(t.purchaseDate)}`);
        console.log(`  Code:       ${t.code}`);
        if (t.used && t.usedAt) {
            console.log(`  Scanned at: ${formatDateTime(t.usedAt)}`);
        }
    }
    console.log(line);
}

// Prints all tickets as a tidy table
function showAllTickets() {
    const rows = listTickets().map(t => ({
        Code: t.code,
        Name: t.name,
        Event: t.event,
        Year: t.year,
        Purchased: formatDate(t.purchaseDate),
        Status: t.used ? 'Used' : 'Unused'
    }));
    console.log('\nAll tickets:');
    console.table(rows);
}

// ---------- Test run ----------

const ticket = createTicket('Summer Concert', 'Jane Smith');
console.log('Created ticket:', ticket.code);

showScan(redeemTicket(ticket.code));            // first scan: valid
showScan(redeemTicket(ticket.code));            // second scan: already used
showScan(redeemTicket('TKT-FAKE-FAKE-FAKE'));   // fake code: invalid

showAllTickets();

// Uncomment to wipe all tickets manually, run once, then comment it again:
// console.log('Deleted tickets:', deleteAllTickets());