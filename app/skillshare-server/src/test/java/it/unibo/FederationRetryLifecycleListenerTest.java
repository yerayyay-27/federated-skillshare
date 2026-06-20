package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.jupiter.api.Test;

class FederationRetryLifecycleListenerTest {

    @Test
    void applicationLifecycleStartsAndStopsScheduler() {
        RecordingScheduler scheduler = new RecordingScheduler();
        FederationRetryLifecycleListener listener =
                new FederationRetryLifecycleListener(scheduler);

        listener.contextInitialized(null);
        listener.contextDestroyed(null);

        assertEquals(1, scheduler.getStartCalls());
        assertEquals(1, scheduler.getStopCalls());
    }

    private static class RecordingScheduler extends FederationRetryScheduler {

        private int startCalls;
        private int stopCalls;

        RecordingScheduler() {
            super(
                    new FederationClient(() -> Collections.emptyList(), null),
                    false,
                    10,
                    new ScheduledThreadPoolExecutor(1));
        }

        @Override
        public synchronized void start() {
            startCalls++;
        }

        @Override
        public synchronized void stop() {
            stopCalls++;
        }

        int getStartCalls() {
            return startCalls;
        }

        int getStopCalls() {
            return stopCalls;
        }
    }
}
