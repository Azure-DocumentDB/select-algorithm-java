package com.azure.documentdb.selectalgorithm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit paid-resource gate. Selecting the profile without consent FAILS, never skips. */
class AzureIT {
    @Test void allNineOnExplicitlyApprovedAzureResource() {
        assertEquals("true", System.getenv("RUN_AZURE_INTEGRATION"),
                "Set RUN_AZURE_INTEGRATION=true only for an authorized isolated Azure resource");
        Config config = Config.from(System.getenv());
        Fixture fixture = Fixture.load(config);
        try (Connections connections = Connections.open(config); MongoStore store = new MongoStore(config, connections)) {
            assertEquals(0, Comparison.run(config, fixture, store, System.out, Thread::sleep), "Every one of nine cases must pass");
            // Second run is idempotent. Independent inspection is not conditioned on document count.
            for (var c : Comparison.cases()) {
                var counts = store.prepare(c, fixture);
                assertEquals(0, counts.inserted()); assertTrue(counts.indexReused());
                assertEquals(fixture.documents().size(), counts.fixture());
                assertTrue(counts.total() >= counts.fixture());
            }
        }
    }
}
