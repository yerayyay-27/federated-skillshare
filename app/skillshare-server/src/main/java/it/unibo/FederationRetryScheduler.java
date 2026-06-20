package it.unibo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Periodically retries federation events that remain in the outbox. */
public class FederationRetryScheduler {

    static final long DEFAULT_INTERVAL_SECONDS = 10;

    private final FederationClient federationClient;
    private final boolean enabled;
    private final long intervalSeconds;
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> retryTask;

    public FederationRetryScheduler() {
        this(
                new FederationClient(),
                readEnabled(),
                readIntervalSeconds(),
                newDaemonExecutor());
    }

    FederationRetryScheduler(
            FederationClient federationClient,
            boolean enabled,
            long intervalSeconds,
            ScheduledExecutorService executor) {
        if (federationClient == null) {
            throw new IllegalArgumentException("Federation client must not be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("Scheduler executor must not be null");
        }
        this.federationClient = federationClient;
        this.enabled = enabled;
        this.intervalSeconds = sanitizeInterval(intervalSeconds);
        this.executor = executor;
    }

    public synchronized void start() {
        if (!enabled) {
            System.out.println("Federation: automatic outbox retry is disabled");
            return;
        }
        if (retryTask != null) {
            return;
        }

        retryTask = executor.scheduleWithFixedDelay(
                this::retrySafely,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS);
        System.out.println("Federation: automatic outbox retry started (every "
                + intervalSeconds + " seconds)");
    }

    public synchronized void stop() {
        if (retryTask != null) {
            retryTask.cancel(true);
            retryTask = null;
        }
        executor.shutdownNow();
        System.out.println("Federation: automatic outbox retry stopped");
    }

    private void retrySafely() {
        try {
            int delivered = federationClient.retryPending();
            if (delivered > 0) {
                System.out.println("Federation: automatic retry delivered "
                        + delivered + " outbox event(s)");
            }
        } catch (Exception e) {
            System.err.println("Federation: automatic outbox retry failed ("
                    + e.getMessage() + ")");
        }
    }

    static boolean parseEnabled(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        if ("false".equalsIgnoreCase(value.trim())) {
            return false;
        }
        return true;
    }

    static long parseIntervalSeconds(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT_INTERVAL_SECONDS;
        }
        try {
            return sanitizeInterval(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return DEFAULT_INTERVAL_SECONDS;
        }
    }

    private static boolean readEnabled() {
        return parseEnabled(readSetting("FEDERATION_RETRY_ENABLED"));
    }

    private static long readIntervalSeconds() {
        return parseIntervalSeconds(readSetting("FEDERATION_RETRY_INTERVAL_SECONDS"));
    }

    private static String readSetting(String key) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(key);
        }
        return value;
    }

    private static long sanitizeInterval(long intervalSeconds) {
        return intervalSeconds < 1 ? DEFAULT_INTERVAL_SECONDS : intervalSeconds;
    }

    private static ScheduledExecutorService newDaemonExecutor() {
        return Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "federation-outbox-retry");
            thread.setDaemon(true);
            return thread;
        });
    }
}
