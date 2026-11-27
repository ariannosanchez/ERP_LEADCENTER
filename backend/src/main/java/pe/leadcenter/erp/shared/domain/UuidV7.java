package pe.leadcenter.erp.shared.domain;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * UUID version 7 (RFC 9562) generator: 48-bit Unix millisecond timestamp, version, a 12-bit counter
 * that keeps ids strictly increasing inside one millisecond, variant and 62 random bits.
 *
 * <p>Pure Java so it can be called from domain code (no Spring, no JPA).
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Object LOCK = new Object();
    private static final int COUNTER_MAX = 0xFFF;

    private static long lastMillis = -1;
    private static int counter;

    private UuidV7() {}

    /** Generates a new time-ordered UUID. */
    public static UUID generate() {
        long millis;
        int sequence;
        synchronized (LOCK) {
            long now = System.currentTimeMillis();
            if (now > lastMillis) {
                lastMillis = now;
                counter = RANDOM.nextInt(1 << 11);
            } else if (counter < COUNTER_MAX) {
                counter++;
            } else {
                // Counter exhausted (or clock moved back): borrow the next millisecond.
                lastMillis++;
                counter = RANDOM.nextInt(1 << 11);
            }
            millis = lastMillis;
            sequence = counter;
        }
        long msb = (millis << 16) | (0x7L << 12) | sequence;
        long lsb = (RANDOM.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(msb, lsb);
    }

    /** Returns the creation instant embedded in a UUIDv7. */
    public static Instant timestampOf(UUID id) {
        if (id.version() != 7) {
            throw new IllegalArgumentException("Not a UUIDv7: " + id);
        }
        return Instant.ofEpochMilli(id.getMostSignificantBits() >>> 16);
    }
}
