package it.unibo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

class FederationRetrySchedulerTest {

    @Test
    void disabledSchedulerDoesNotScheduleRetryTask() {
        RecordingScheduledExecutor executor = new RecordingScheduledExecutor();
        FederationRetryScheduler scheduler = new FederationRetryScheduler(
                new TestFederationClient(), false, 10, executor);

        scheduler.start();

        assertEquals(0, executor.getScheduleCalls());
        scheduler.stop();
    }

    @Test
    void enabledSchedulerStartsOnlyOneRetryTask() {
        RecordingScheduledExecutor executor = new RecordingScheduledExecutor();
        FederationRetryScheduler scheduler = new FederationRetryScheduler(
                new TestFederationClient(), true, 10, executor);

        scheduler.start();
        scheduler.start();

        assertEquals(1, executor.getScheduleCalls());
        assertEquals(10, executor.getInitialDelaySeconds());
        assertEquals(10, executor.getDelaySeconds());
        scheduler.stop();
    }

    @Test
    void invalidIntervalsFallBackToDefault() {
        assertEquals(FederationRetryScheduler.DEFAULT_INTERVAL_SECONDS,
                FederationRetryScheduler.parseIntervalSeconds("invalid"));
        assertEquals(FederationRetryScheduler.DEFAULT_INTERVAL_SECONDS,
                FederationRetryScheduler.parseIntervalSeconds("0"));
        assertEquals(FederationRetryScheduler.DEFAULT_INTERVAL_SECONDS,
                FederationRetryScheduler.parseIntervalSeconds("-2"));
        assertEquals(25, FederationRetryScheduler.parseIntervalSeconds("25"));
    }

    @Test
    void scheduledTaskRetriesAndSurvivesClientExceptions() {
        RecordingScheduledExecutor executor = new RecordingScheduledExecutor();
        TestFederationClient client = new TestFederationClient();
        client.failNextRetry();
        FederationRetryScheduler scheduler = new FederationRetryScheduler(
                client, true, 10, executor);
        scheduler.start();

        assertDoesNotThrow(() -> executor.getScheduledTask().run());
        assertDoesNotThrow(() -> executor.getScheduledTask().run());

        assertEquals(2, client.getRetryCalls());
        scheduler.stop();
    }

    @Test
    void stopCancelsTaskAndShutsDownExecutor() {
        RecordingScheduledExecutor executor = new RecordingScheduledExecutor();
        FederationRetryScheduler scheduler = new FederationRetryScheduler(
                new TestFederationClient(), true, 10, executor);
        scheduler.start();

        scheduler.stop();

        assertTrue(executor.getFuture().isCancelled());
        assertTrue(executor.isShutdown());
    }

    private static class TestFederationClient extends FederationClient {

        private int retryCalls;
        private boolean failNextRetry;

        TestFederationClient() {
            super(() -> Collections.emptyList(), null);
        }

        @Override
        public int retryPending() {
            retryCalls++;
            if (failNextRetry) {
                failNextRetry = false;
                throw new IllegalStateException("test failure");
            }
            return 0;
        }

        void failNextRetry() {
            failNextRetry = true;
        }

        int getRetryCalls() {
            return retryCalls;
        }
    }

    private static class RecordingScheduledExecutor extends ScheduledThreadPoolExecutor {

        private final RecordingScheduledFuture future = new RecordingScheduledFuture();
        private Runnable scheduledTask;
        private int scheduleCalls;
        private long initialDelaySeconds;
        private long delaySeconds;

        RecordingScheduledExecutor() {
            super(1);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(
                Runnable command,
                long initialDelay,
                long delay,
                TimeUnit unit) {
            scheduledTask = command;
            scheduleCalls++;
            initialDelaySeconds = unit.toSeconds(initialDelay);
            delaySeconds = unit.toSeconds(delay);
            return future;
        }

        @Override
        public List<Runnable> shutdownNow() {
            super.shutdownNow();
            return Collections.emptyList();
        }

        Runnable getScheduledTask() {
            return scheduledTask;
        }

        int getScheduleCalls() {
            return scheduleCalls;
        }

        long getInitialDelaySeconds() {
            return initialDelaySeconds;
        }

        long getDelaySeconds() {
            return delaySeconds;
        }

        RecordingScheduledFuture getFuture() {
            return future;
        }
    }

    private static class RecordingScheduledFuture implements ScheduledFuture<Object> {

        private boolean cancelled;

        @Override
        public long getDelay(TimeUnit unit) {
            return Long.MAX_VALUE;
        }

        @Override
        public int compareTo(Delayed other) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return cancelled;
        }

        @Override
        public Object get() throws InterruptedException, ExecutionException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            throw new UnsupportedOperationException();
        }
    }
}
