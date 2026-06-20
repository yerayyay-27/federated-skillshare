package it.unibo;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/** Starts and stops the application-wide federation retry scheduler. */
public class FederationRetryLifecycleListener implements ServletContextListener {

    private final FederationRetryScheduler scheduler;

    public FederationRetryLifecycleListener() {
        this(new FederationRetryScheduler());
    }

    FederationRetryLifecycleListener(FederationRetryScheduler scheduler) {
        if (scheduler == null) {
            throw new IllegalArgumentException("Federation retry scheduler must not be null");
        }
        this.scheduler = scheduler;
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        scheduler.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        scheduler.stop();
    }
}
