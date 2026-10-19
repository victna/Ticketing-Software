const { createTicket, redeemTicket, listTickets } = require('./ticket.js');

// 1. Create a ticket
const ticket = createTicket('Summer Concert', 'Jane Smith');
console.log('Created ticket:', ticket.code);

// 2. Scan it once (should work)
console.log('First scan: ', redeemTicket(ticket.code));

// 3. Scan it again (should be rejected)
console.log('Second scan:', redeemTicket(ticket.code));

// 4. Scan a code that doesn't exist (should be rejected)
console.log('Fake code:  ', redeemTicket('TKT-FAKE-FAKE-FAKE'));

// 5. Show everything stored in the database
console.table(listTickets());
