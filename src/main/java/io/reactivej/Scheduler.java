package io.reactivej;

import java.io.Serializable;
import java.util.concurrent.*;

/***
 * @author heartup@gmail.com
 */
public class Scheduler {

    private final ReactiveSystem system;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);

    public Scheduler(ReactiveSystem system) {
        this.system = system;
    }

    public void scheduleOnce(long delay, final ReactiveRef receiver, final Serializable message, final ReactiveRef sender) {
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                system.sendMessage(receiver, message, sender);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    public ScheduledFuture<?> schedule(long delay, long interval, final ReactiveRef receiver, final Serializable message, final ReactiveRef sender) {
        return executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                system.sendMessage(receiver, message, sender);
            }
        }, delay, interval, TimeUnit.MILLISECONDS);
    }
}
