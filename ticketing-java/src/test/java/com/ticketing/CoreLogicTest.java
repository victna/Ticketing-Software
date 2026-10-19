package com.ticketing;

import com.ticketing.pass.Perk;
import com.ticketing.pass.PassType;
import com.ticketing.security.EncryptionService;
import com.ticketing.util.CodeGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Fast checks that need no database or web server. */
class CoreLogicTest {

    @TempDir
    Path tempDir;

    @Test
    void encryptionRoundTripWorks() throws IOException {
        EncryptionService crypto = new EncryptionService(EncryptionService.generateKeyBase64(), tempDir.toString());
        String secret = crypto.encryptText("Jane Smith", "ticket:name:TKT-AAAA-BBBB-CCCC");

        assertNotEquals("Jane Smith", secret);
        assertEquals("Jane Smith", crypto.decryptText(secret, "ticket:name:TKT-AAAA-BBBB-CCCC"));
    }

    @Test
    void encryptedValueCannotBeMovedToAnotherRecord() throws IOException {
        EncryptionService crypto = new EncryptionService(EncryptionService.generateKeyBase64(), tempDir.toString());
        String secret = crypto.encryptText("Jane Smith", "ticket:name:TKT-AAAA-BBBB-CCCC");

        assertThrows(IllegalStateException.class,
                () -> crypto.decryptText(secret, "ticket:name:TKT-ZZZZ-ZZZZ-ZZZZ"));
    }

    @Test
    void tamperedDataIsRejected() throws IOException {
        EncryptionService crypto = new EncryptionService(EncryptionService.generateKeyBase64(), tempDir.toString());
        byte[] encrypted = crypto.encrypt("photo bytes".getBytes(StandardCharsets.UTF_8), "ctx");
        encrypted[encrypted.length - 1] ^= 1;

        assertThrows(IllegalStateException.class, () -> crypto.decrypt(encrypted, "ctx"));
    }

    @Test
    void generatedKeyFileIsReusedOnNextStart() throws IOException {
        EncryptionService first = new EncryptionService("", tempDir.toString());
        String secret = first.encryptText("Jane Smith", "ctx");

        EncryptionService second = new EncryptionService("", tempDir.toString());
        assertEquals("Jane Smith", second.decryptText(secret, "ctx"));
    }

    @Test
    void codesHaveTheExpectedShape() {
        CodeGenerator generator = new CodeGenerator();
        String code = generator.generate("TKT");

        assertTrue(code.matches("TKT-[A-HJKMNP-Z2-9]{4}-[A-HJKMNP-Z2-9]{4}-[A-HJKMNP-Z2-9]{4}"), code);
        assertNotEquals(code, generator.generate("TKT"));
        assertEquals("TKT-ABCD-EFGH-JKMN", CodeGenerator.normalize("  tkt-abcd-efgh-jkmn "));
    }

    @Test
    void passTypesFollowTheRules() {
        assertTrue(PassType.LEGEND.validAt("Any Park At All"));
        assertTrue(PassType.HERO.validAt("worlds of fun"));
        assertFalse(PassType.EXPLORER.validAt("Worlds of Fun"));

        assertEquals(4, PassType.LEGEND.limit(Perk.FAST_LANE));
        assertEquals(0, PassType.HERO.limit(Perk.FAST_LANE));
        assertEquals(1, PassType.HERO.limit(Perk.FRIEND_DISCOUNT));

        assertEquals(PassType.LEGEND, PassType.fromName(" legend "));
        assertThrows(IllegalArgumentException.class, () -> PassType.fromName("gold"));
        assertEquals(Perk.FAST_LANE, Perk.fromName("fastLane"));
    }
}
