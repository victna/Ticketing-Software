const readline = require('readline/promises');
const { redeemTicket } = require('./ticket.js');
const { showTicket } = require('./display.js');

async function main() {
    const rl = readline.createInterface({ input: process.stdin, output: process.stdout });

    console.log('=== TICKET SCANNER ===');
    while (true) {
        const code = (await rl.question('Scan or type a code (press Enter to quit): ')).trim();
        if (!code) break;

        const result = redeemTicket(code);
        if (result.ok) {
            showTicket('VALID TICKET', result.ticket);
        } else {
            showTicket(`REJECTED: ${result.reason}`, result.ticket);
        }
    }

    rl.close();
}

main();