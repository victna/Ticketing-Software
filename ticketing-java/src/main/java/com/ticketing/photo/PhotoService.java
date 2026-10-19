package com.ticketing.photo;

import com.ticketing.security.EncryptionService;
import com.ticketing.util.CodeGenerator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Stores one encrypted photo per pass. */
@Service
public class PhotoService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_PHOTO_BYTES = 5 * 1024 * 1024;   // 5 MB

    public record PhotoData(String mimeType, byte[] image, Instant takenAt) {
    }

    public record SaveResult(boolean ok, String reason) {
    }

    private final JdbcTemplate jdbc;
    private final EncryptionService crypto;

    public PhotoService(JdbcTemplate jdbc, EncryptionService crypto) {
        this.jdbc = jdbc;
        this.crypto = crypto;
    }

    private static String context(String code) {
        return "photo:image:" + code;
    }

    /** Saves (or replaces) the photo for a pass. */
    public SaveResult setPhoto(String code, byte[] image, String contentType) {
        String normalized = CodeGenerator.normalize(code);

        Integer passCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM passes WHERE code = ?", Integer.class, normalized);
        if (passCount == null || passCount == 0) {
            return new SaveResult(false, "Invalid pass");
        }

        String mimeType = contentType == null ? "" : contentType.split(";")[0].trim().toLowerCase();
        if (!ALLOWED_TYPES.contains(mimeType)) {
            return new SaveResult(false, "Photo must be a JPEG, PNG, or WebP image");
        }
        if (image == null || image.length == 0) {
            return new SaveResult(false, "No image received");
        }
        if (image.length > MAX_PHOTO_BYTES) {
            return new SaveResult(false, "Photo is too large (5 MB max)");
        }
        if (!looksLike(mimeType, image)) {
            return new SaveResult(false, "The file is not a real " + mimeType + " image");
        }

        jdbc.update("""
                INSERT INTO photos (code, mime_type, image, taken_at) VALUES (?, ?, ?, ?)
                ON CONFLICT(code) DO UPDATE SET
                  mime_type = excluded.mime_type,
                  image = excluded.image,
                  taken_at = excluded.taken_at
                """,
                normalized, mimeType, crypto.encrypt(image, context(normalized)), Instant.now().toString());

        return new SaveResult(true, null);
    }

    public Optional<PhotoData> getPhoto(String code) {
        String normalized = CodeGenerator.normalize(code);
        List<PhotoData> rows = jdbc.query(
                "SELECT mime_type, image, taken_at FROM photos WHERE code = ?",
                (rs, i) -> new PhotoData(
                        rs.getString("mime_type"),
                        crypto.decrypt(rs.getBytes("image"), context(normalized)),
                        Instant.parse(rs.getString("taken_at"))),
                normalized);
        return rows.stream().findFirst();
    }

    public boolean hasPhoto(String code) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM photos WHERE code = ?", Integer.class, CodeGenerator.normalize(code));
        return count != null && count > 0;
    }

    /** Checks the first bytes of the file, so a renamed text file can't be stored as a photo. */
    static boolean looksLike(String mimeType, byte[] b) {
        switch (mimeType) {
            case "image/jpeg":
                return b.length > 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
            case "image/png":
                return b.length > 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "image/webp":
                return b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                        && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default:
                return false;
        }
    }
}
