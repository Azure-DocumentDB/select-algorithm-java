package com.azure.documentdb.selectalgorithm;

import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
    interface StoreFactory { Comparison.Store open(Config config); }
    public static void main(String[] args) {
        // SDK exception/diagnostic logs can carry endpoints or document contents. Emit our safe errors only.
        Logger.getLogger("org.mongodb.driver").setLevel(Level.OFF);
        Logger.getLogger("com.azure").setLevel(Level.OFF);
        int status = run(System.getenv(), Main::openStore, System.out, System.err);
        if (status != 0) System.exit(status);
    }
    private static Comparison.Store openStore(Config config) {
        Connections connections = Connections.open(config);
        try { return new MongoStore(config, connections); }
        catch (RuntimeException | Error failure) { connections.close(); throw failure; }
    }
    static int run(Map<String, String> env, StoreFactory factory, PrintStream out, PrintStream err) {
        Config config;
        Fixture fixture;
        try { config = Config.from(env); fixture = Fixture.load(config); }
        catch (RuntimeException e) {
            err.println("Configuration/fixture validation failed; check required settings and input files. No connection attempted.");
            return 2;
        }
        out.println("Nine independent cases; scores are not comparable across metrics. No model calls; no deletion or reset.");
        try (Comparison.Store store = factory.open(config)) {
            return Comparison.run(config, fixture, store, out, Thread::sleep);
        } catch (Exception e) { err.println(Comparison.safeError(e)); return 1; }
    }
}
