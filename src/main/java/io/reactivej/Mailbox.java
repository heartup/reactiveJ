package io.reactivej;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author heartup@gmail.com
 *
 */
public class Mailbox implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(Mailbox.class);

    public static int DEFAULT_THROUGHPUT = 5;

    /**
     * 0:idle 1: executing
     */
    private final AtomicInteger status = new AtomicInteger(0);

    private final ConcurrentLinkedDeque<Envelope> systemMessageQueue = new ConcurrentLinkedDeque<>();

    private final ConcurrentLinkedDeque<Envelope> queue = new ConcurrentLinkedDeque<>();

    private final ReactiveCell componentCell;

    public Mailbox(ReactiveCell component) {
        this.componentCell = component;
    }

    public ConcurrentLinkedDeque<Envelope> getSystemMessageQueue() {
        return systemMessageQueue;
    }

    public ConcurrentLinkedDeque<Envelope> getQueue() {
        return queue;
    }

    public void setAsIdle() {
        status.set(0);
    }

    public boolean setAsScheduled() {
        return status.compareAndSet(0, 1);
    }

    public ReactiveCell getComponentCell() {
        return componentCell;
    }

    @Override
    public void run() {
        try {
            if (isScheduled()) {  // piggybacking on synchronization makes it behave as one thread
                processAllSystemMessages();
                processMailbox();
            }
        }
        finally {
            setAsIdle();  // piggybacking on synchronization makes it behave as one thread
            getComponentCell().getDispatcher().registerForExecution(this, false);
        }
    }

    public void processAllSystemMessages() {
        while (!getSystemMessageQueue().isEmpty()) {
            Envelope env = getSystemMessageQueue().poll();
            componentCell.setCurrentMessage(env);
            componentCell.setSender(env.getSender());
            try {
                if (env.getMessage() instanceof Failure) {
                    componentCell.getComponent().onSupervise((Failure) env.getMessage());
                }
                else if (env.getMessage() instanceof Poison) {
                    String compPath = getComponentCell().getSelf().getPath();
                    String[] pathSegs = compPath.split(ReactiveSystem.componentSplitter);
                    String compName = pathSegs[pathSegs.length - 1];

                    getComponentCell().getParent().getCell().destroyChild(compName);
                }
                else {
                    componentCell.getComponent().receiveMessage(env.getMessage());
                }
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            catch (Exception e) {
                logger.error("system message [" + env.toString() + "] exception", e);
            }
        }
    }

    public void processMailbox() {
        if (logger.isDebugEnabled()) {
            logger.debug("queue of [{}] pending message [{}]", getComponentCell().getSelf(), getQueue().size());
        }

        int processed = 0;
        while (processed < DEFAULT_THROUGHPUT && !getQueue().isEmpty()) {
            Envelope env = getQueue().poll();
            componentCell.setCurrentMessage(env);
            componentCell.setSender(env.getSender());
            try {
                componentCell.getComponent().receiveMessage(env.getMessage());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            catch (Exception e) {
                handleFailure(env, e);
            }

            processed ++;
        }
    }

    private void handleFailure(Envelope env, Exception cause) {
        componentCell.getParent().tell(new Failure(env, cause), componentCell.getSelf());
    }

    public boolean isScheduled() {
        return status.get() == 1;
    }
}
