package com.ticketing.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Tells the web page which addresses a phone or tablet should use, and prints them at startup. */
@RestController
public class ServerInfoController {

    private static final Logger log = LoggerFactory.getLogger(ServerInfoController.class);

    public record ServerInfo(int port, List<String> lanUrls) {
    }

    private final int port;

    public ServerInfoController(@Value("${server.port:8080}") int port) {
        this.port = port;
    }

    @GetMapping("/api/server-info")
    public ServerInfo info() {
        List<String> urls = NetworkUtil.lanAddresses().stream()
                .map(ip -> "http://" + ip + ":" + port)
                .toList();
        return new ServerInfo(port, urls);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printAddresses() {
        log.info("Open on this computer: http://localhost:{}", port);
        for (String url : info().lanUrls()) {
            log.info("Open from a phone or tablet on the same Wi-Fi: {}", url);
        }
    }
}
