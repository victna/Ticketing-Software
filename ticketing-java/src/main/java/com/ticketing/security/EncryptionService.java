package com.ticketing.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption for data stored in the database (holder names and photos).
 *
 * Each value gets its own random 12-byte IV, stored in front of the ciphertext.
 * A "context" string (for example "pass:name:PASS-ABCD-...") is authenticated along
 * with the data, so an encrypted value copied onto a different record fails to decrypt.
 *
 * The key comes from the TICKETING_ENCRYPTION_KEY environment variable (Base64, 32 bytes).
 * If that is not set, a key file is created at data/secret.key on first start.
 */
@Service
public class EncryptionService {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey key;
    private final SecureRandom random = new SecureRandom();

    public EncryptionService(@Value("${ticketing.encryption-key:}") String base64Key,
                             @Value("${ticketing.data-dir:data}") String dataDir) throws IOException {
        this.key = loadKey(base64Key == null ? "" : base64Key.trim(), Path.of(dataDir));
    }

    private static SecretKey loadKey(String fromEnvironment, Path dataDir) throws IOException {
        byte[] raw;
        if (!fromEnvironment.isEmpty()) {
            raw = Base64.getDecoder().decode(fromEnvironment);
        } else {
            Path file = dataDir.resolve("secret.key");
            if (Files.exists(file)) {
                raw = Base64.getDecoder().decode(Files.readString(file).trim());
            } else {
                Files.createDirectories(dataDir);
                raw = new byte[32];
                new SecureRandom().nextBytes(raw);
                Files.writeString(file, Base64.getEncoder().encodeToString(raw));
                try {
                    Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
                } catch (UnsupportedOperationException ignored) {
                    // Windows: file permissions work differently, nothing to do here
                }
            }
        }
        if (raw.length != 32) {
            throw new IllegalStateException(
                    "The encryption key must be exactly 32 bytes (Base64 encoded), but it was " + raw.length + " bytes.");
        }
        return new SecretKeySpec(raw, "AES");
    }

    /** Creates a new random key as Base64 text, for use as TICKETING_ENCRYPTION_KEY. */
    public static String generateKeyBase64() {
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        return Base64.getEncoder().encodeToString(raw);
    }

    public byte[] encrypt(byte[] plain, String context) {
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(plain);

            byte[] out = new byte[IV_BYTES + encrypted.length];
            System.arraycopy(iv, 0, out, 0, IV_BYTES);
            System.arraycopy(encrypted, 0, out, IV_BYTES, encrypted.length);
            return out;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] data, String context) {
        if (data == null || data.length <= IV_BYTES) {
            throw new IllegalStateException("Stored encrypted value is missing or too short");
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, data, 0, IV_BYTES));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(data, IV_BYTES, data.length - IV_BYTES);
        } catch (GeneralSecurityException e) {
            // Wrong key, damaged data, or data that belongs to a different record
            throw new IllegalStateException("Could not decrypt stored data (wrong key or tampered data)", e);
        }
    }

    public String encryptText(String text, String context) {
        byte[] encrypted = encrypt(text.getBytes(StandardCharsets.UTF_8), context);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    public String decryptText(String base64, String context) {
        byte[] plain = decrypt(Base64.getDecoder().decode(base64), context);
        return new String(plain, StandardCharsets.UTF_8);
    }
}
