package io.reactivej;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/***
 * @author heartup@gmail.com
 */
public class Dispatcher {
    private static Logger logger = LoggerFactory.getLogger(Dispatcher.class);

    private final ExecutorService executorService;

    public Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void dispatch(ReactiveCell receiver, Envelope envlop) {
        if (envlop.getMessage() instanceof SystemMessage) {
            // 系统消息最近的优先处理
            receiver.getMailbox().getSystemMessageQueue().addFirst(envlop);
        } else {
            receiver.getMailbox().getQueue().offer(envlop);
        }

        receiver.getDispatcher().registerForExecution(receiver.getMailbox(), true);
    }

    public void registerForExecution(Mailbox mailbox, boolean hasNewMsg) {
        // 有待处理的消息的时候才把任务放入线程池
        if (hasNewMsg
                || !mailbox.getSystemMessageQueue().isEmpty()
                || !mailbox.getQueue().isEmpty()) {
            // 每个mailbox的消息处理要串行化
            if (mailbox.setAsScheduled()) {
                getExecutorService().execute(mailbox);
            }
        }
    }
}
