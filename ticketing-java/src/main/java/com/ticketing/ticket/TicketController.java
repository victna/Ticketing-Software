package com.ticketing.ticket;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Replaces create.js and scan.js with web endpoints. */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    public record CreateTicketRequest(String event, String name, Integer year) {
    }

    private final TicketService tickets;

    public TicketController(TicketService tickets) {
        this.tickets = tickets;
    }

    @PostMapping
    public Ticket create(@RequestBody CreateTicketRequest request) {
        return tickets.createTicket(request.event(), request.name(), request.year());
    }

    @PostMapping("/{code}/redeem")
    public RedeemResult redeem(@PathVariable String code) {
        return tickets.redeemTicket(code);
    }

    @GetMapping
    public List<Ticket> list() {
        return tickets.listTickets();
    }

    /** Needs ?confirm=DELETE-ALL so it can't be triggered by accident. */
    @DeleteMapping
    public ResponseEntity<Map<String, Object>> deleteAll(@RequestParam(required = false) String confirm) {
        if (!"DELETE-ALL".equals(confirm)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("ok", false, "reason", "Add ?confirm=DELETE-ALL to delete every ticket"));
        }
        return ResponseEntity.ok(Map.of("ok", true, "deleted", tickets.deleteAllTickets()));
    }
}
