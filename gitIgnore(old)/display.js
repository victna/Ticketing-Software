function formatDate(iso) {
    return new Date(iso).toLocaleDateString('en-US', {
        year: 'numeric', month: 'short', day: 'numeric'
    });
}

function formatDateTime(iso) {
    return new Date(iso).toLocaleString('en-US', {
        year: 'numeric', month: 'short', day: 'numeric',
        hour: 'numeric', minute: '2-digit'
    });
}

const line = '-'.repeat(40);

// Shows a ticket's details, with a heading above it
function showTicket(heading, t) {
    console.log('\n' + line);
    console.log('  ' + heading);
    console.log(line);
    if (t) {
        console.log(`  Name:       ${t.name}`);
        console.log(`  Event:      ${t.event}`);
        console.log(`  Year:       ${t.year}`);
        console.log(`  Purchased:  ${formatDate(t.purchaseDate)}`);
        console.log(`  Code:       ${t.code}`);
        if (t.used && t.usedAt) {
            console.log(`  Scanned at: ${formatDateTime(t.usedAt)}`);
        }
    }
    console.log(line + '\n');
}

module.exports = { showTicket };