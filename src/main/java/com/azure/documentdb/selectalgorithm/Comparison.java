package com.azure.documentdb.selectalgorithm;

import org.bson.Document;
import com.mongodb.MongoException;
import java.io.PrintStream;
import java.util.*;

/** Nine independent cases. An individual failure never becomes a successful zero score. */
final class Comparison {
    record Case(String algorithm, String metric) { }
    record Counts(long total, long fixture, int inserted, boolean indexReused) { }
    interface Store extends AutoCloseable {
        Counts prepare(Case c, Fixture fixture);
        List<Document> search(Case c, List<Double> query);
        @Override void close();
    }
    interface Sleeper { void sleep(long millis) throws InterruptedException; }
    static List<Case> cases() {
        List<Case> cases = new ArrayList<>();
        for (String algo : List.of("ivf", "hnsw", "diskann"))
            for (String metric : List.of("COS", "L2", "IP")) cases.add(new Case(algo, metric));
        return List.copyOf(cases);
    }
    static int run(Config config, Fixture fixture, Store store, PrintStream out, Sleeper sleeper) {
        int succeeded = 0;
        for (Case c : cases()) {
            Document row = new Document("algorithm", c.algorithm()).append("metric", c.metric())
                    .append("collection", config.collection(c));
            String stage = "prepare";
            try {
                Counts counts = store.prepare(c, fixture);
                row.append("totalDocuments", counts.total()).append("fixtureDocuments", counts.fixture())
                        .append("inserted", counts.inserted()).append("indexReused", counts.indexReused());
                stage = "search";
                List<Document> hits = search(config, fixture, store, c, sleeper);
                validateHits(hits, config.topK());
                row.append("status", "PASS").append("hits", hits);
                succeeded++;
            } catch (Exception e) {
                if (e instanceof InterruptedException) Thread.currentThread().interrupt();
                row.append("status", "FAIL").append("stage", stage).append("error", safeError(e));
            }
            out.println(row.toJson());
        }
        out.println(new Document("summary", new Document("attempted", 9).append("succeeded", succeeded)
                .append("failed", 9 - succeeded)).toJson());
        return succeeded == 9 ? 0 : 1;
    }
    static List<Document> search(Config config, Fixture fixture, Store store, Case c, Sleeper sleeper) throws InterruptedException {
        for (int attempt = 1; attempt <= config.attempts(); attempt++) {
            try {
                List<Document> hits = store.search(c, fixture.query());
                if (!hits.isEmpty()) return hits;
                if (attempt == config.attempts()) throw new IllegalStateException("empty results");
            } catch (MongoException e) {
                if (attempt == config.attempts()) throw e;
            }
            sleeper.sleep(config.retryDelayMs());
        }
        throw new IllegalStateException("search failed");
    }
    static void validateHits(List<Document> hits, int topK) {
        if (hits.size() != topK) throw new IllegalStateException("unexpected result count");
        Set<Object> ids = new HashSet<>();
        for (Document hit : hits) {
            if (hit.get("_id") == null || !ids.add(hit.get("_id")) || !(hit.get("score") instanceof Number score)
                    || !Double.isFinite(score.doubleValue())) throw new IllegalStateException("invalid result");
        }
    }
    static String safeError(Exception e) {
        // Exception messages/stack traces can include hostnames, credentials or document content.
        if (e instanceof MongoException mongo) return "MongoDB operation failed (code " + mongo.getCode() + ")";
        if (e instanceof InterruptedException) return "interrupted";
        if (e instanceof IllegalStateException) return "data/index conflict or invalid/empty result";
        return "operation failed";
    }
}
