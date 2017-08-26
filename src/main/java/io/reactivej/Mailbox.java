package io.reactivej;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Unsafe;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * @author heartup@gmail.com
 *
 * 具有保序处理和单线程处理的内存模型语义
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

    /**
     * 如果已经在执行返回false，否则抢占设置为1，设置成功的是true
     * @return
     */
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
                ClassLoader systemClassLoader = getComponentCell().getSystem().getSystemClassLoader();
                if (systemClassLoader != null && Thread.currentThread().getContextClassLoader() != systemClassLoader) {
                    Thread.currentThread().setContextClassLoader(systemClassLoader);
                }

                processAllSystemMessages();
                processMailbox();
            }
        }
        finally {
            setAsIdle();  // piggybacking on synchronization makes it behave as one thread
            // 没有新消息，但是要检查queue里面有没有新到消息
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
                Thread.currentThread().interrupt();  //  让后续代码处理线程中断
            }
            catch (Throwable e) {
                handleFailure(env, e);
            }
        }
    }

    public void processMailbox() {
        if (logger.isDebugEnabled()) {
            logger.debug("队列[{}]中待处理的消息还有[{}]", getComponentCell().getSelf(), getQueue().size());
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
                Thread.currentThread().interrupt();  //  让后续代码处理线程中断
            }
            catch (Throwable e) {
                handleFailure(env, e);
            }
            processed ++;
        }
    }

    private void handleFailure(Envelope env, Throwable cause) {
        componentCell.getParent().tell(new Failure(env, cause), componentCell.getSelf());
    }

    public boolean isScheduled() {
        return status.get() == 1;
    }
}
