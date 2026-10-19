package com.ticketing.pass;

import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/** Replaces createPass.js and scanPass.js with web endpoints. */
@RestController
@RequestMapping("/api/passes")
public class PassController {

    public record CreatePassRequest(String type, String name, Integer season) {
    }

    public record PassTypeInfo(String id, String label, double price, String parks) {
    }

    private final PassService passes;

    public PassController(PassService passes) {
        this.passes = passes;
    }

    /** The three pass types, for the dropdown on the web page. */
    @GetMapping("/types")
    public List<PassTypeInfo> types() {
        return Arrays.stream(PassType.values())
                .map(t -> new PassTypeInfo(
                        t.name().toLowerCase(),
                        t.label(),
                        t.price(),
                        t.allParks() ? "All 8 parks" : String.join(", ", t.parks())))
                .toList();
    }

    @PostMapping
    public Pass create(@RequestBody CreatePassRequest request) {
        return passes.createPass(request.type(), request.name(), request.season());
    }

    @GetMapping
    public List<Pass> list() {
        return passes.listPasses();
    }

    @PostMapping("/{code}/checkin")
    public PassResult checkIn(@PathVariable String code, @RequestParam String park) {
        return passes.checkIn(code, park);
    }

    @PostMapping("/{code}/perks/{perk}")
    public PassResult usePerk(@PathVariable String code, @PathVariable String perk) {
        return passes.usePerk(code, perk);
    }

    @GetMapping("/{code}/visits")
    public List<Visit> visits(@PathVariable String code) {
        return passes.listVisits(code);
    }
}
