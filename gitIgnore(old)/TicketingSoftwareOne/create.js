const readline = require('readline/promises');
const { createTicket } = require('./ticket.js');
const { showTicket } = require('./display.js');

async function main() {
    const rl = readline.createInterface({ input: process.stdin, output: process.stdout });
    const currentYear = new Date().getFullYear();

    console.log('=== CREATE TICKETS ===');
    const event = (await rl.question('Event name: ')).trim();
    if (!event) {
        console.log('An event name is required.');
        rl.close();
        return;
    }

    while (true) {
        const name = (await rl.question('\nTicket holder name (press Enter to finish): ')).trim();
        if (!name) break;

        const yearInput = (await rl.question(`Year [${currentYear}]: `)).trim();
        const year = yearInput ? Number(yearInput) : currentYear;
        if (!Number.isInteger(year) || year < 2000 || year > 2100) {
            console.log('That is not a valid year. Ticket not created.');
            continue;
        }

        const ticket = createTicket(event, name, year);
        showTicket('TICKET CREATED', ticket);
    }

    console.log('Done.');
    rl.close();
}

main();