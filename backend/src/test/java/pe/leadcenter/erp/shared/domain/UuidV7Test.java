package pe.leadcenter.erp.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    @Test
    void setsVersion7AndRfcVariant() {
        for (int i = 0; i < 1_000; i++) {
            UUID id = UuidV7.generate();
            assertThat(id.version()).isEqualTo(7);
            assertThat(id.variant()).isEqualTo(2);
        }
    }

    @Test
    void embedsCurrentUnixMillisecondsInTheFirst48Bits() {
        Instant before = Instant.now().minusMillis(1);
        UUID id = UuidV7.generate();
        Instant after = Instant.now().plusMillis(1);

        assertThat(UuidV7.timestampOf(id)).isBetween(before, after);
    }

    @Test
    void isStrictlyIncreasingAsTextAndAsUnsignedBitsEvenWithinTheSameMillisecond() {
        UUID previous = UuidV7.generate();
        for (int i = 0; i < 20_000; i++) {
            UUID next = UuidV7.generate();
            assertThat(next.toString()).isGreaterThan(previous.toString());
            assertThat(next.getMostSignificantBits())
                    .isGreaterThanOrEqualTo(previous.getMostSignificantBits());
            previous = next;
        }
    }

    @Test
    void isUniqueAcrossConcurrentCallers() throws Exception {
        Set<UUID> ids = java.util.concurrent.ConcurrentHashMap.newKeySet();
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int t = 0; t < 8; t++) {
                futures.add(
                        executor.submit(
                                () -> {
                                    for (int i = 0; i < 5_000; i++) {
                                        ids.add(UuidV7.generate());
                                    }
                                }));
            }
            for (var f : futures) {
                f.get();
            }
        }
        assertThat(new HashSet<>(ids)).hasSize(40_000);
    }
}
