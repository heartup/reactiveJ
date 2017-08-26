package io.reactivej;


import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class RootComponent extends ReactiveComponent {

    private boolean suspended;

    @Override
    public void onSupervise(SystemMessage msg) {
        super.onSupervise(msg);
    }

    @Override
    public void preStart() {
        super.preStart();
        long heartbeat = 3000;
        getContext().getSystem().getScheduler().schedule(heartbeat, heartbeat, getSelf(), new CheckSystemLoad(), null);
    }

    @Override
    public AbstractComponentBehavior getDefaultBehavior() {
        return new AbstractComponentBehavior(this) {
            @Override
            public void onMessage(Serializable msg) throws Exception {
                if (msg instanceof CheckSystemLoad) {
                    onCheckSystemLoad((CheckSystemLoad) msg);
                }
            }
        };
    }

    private void onCheckSystemLoad(CheckSystemLoad msg) {
        Runtime rt = Runtime.getRuntime();
        long maxMemory = rt.maxMemory();
        long totalMemory = rt.totalMemory();
        long freeMemory = rt.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        double perc = usedMemory * 1.0 / maxMemory;

        if (!suspended && perc > 0.95) {
            getContext().getSystem().getTransporter().suspendReadingMessage();
            suspended = true;
        }

        if (suspended && perc <= 0.95) {
            getContext().getSystem().getTransporter().resumeReadingMessage();
            suspended = false;
        }
    }
}
