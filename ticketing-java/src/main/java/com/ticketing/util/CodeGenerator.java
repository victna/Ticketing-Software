package com.ticketing.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

/** Makes random, unguessable codes like TKT-7KQ2-M9XF-4RHD. */
@Component
public class CodeGenerator {

    // Letters and digits without look-alikes (no 0/O, 1/I/L)
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private final SecureRandom random = new SecureRandom();

    public String generate(String prefix) {
        StringBuilder sb = new StringBuilder(prefix);
        for (int group = 0; group < 3; group++) {
            sb.append('-');
            for (int i = 0; i < 4; i++) {
                // nextInt(bound) avoids the slight bias of using a modulo on random bytes
                sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
            }
        }
        return sb.toString();
    }

    /** Scanners and people may type lowercase or add spaces. */
    public static String normalize(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }
}
