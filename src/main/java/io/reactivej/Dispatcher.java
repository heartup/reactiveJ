package io.reactivej;

import java.util.concurrent.ExecutorService;

/***
 * @author heartup@gmail.com
 */
public class Dispatcher {

    private final ExecutorService executorService;

    public Dispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void dispatch(ReactiveCell receiver, Envelope envlop) {
        if (envlop.getMessage() instanceof SystemMessage) {
            receiver.getMailbox().getSystemMessageQueue().addFirst(envlop);
        } else {
            receiver.getMailbox().getQueue().offer(envlop);
        }

        receiver.getDispatcher().registerForExecution(receiver.getMailbox(), true);
    }

    public void registerForExecution(Mailbox mailbox, boolean hasNewMsg) {
        if (hasNewMsg
                || !mailbox.getSystemMessageQueue().isEmpty()
                || !mailbox.getQueue().isEmpty()) {
            if (mailbox.setAsScheduled()) {
                getExecutorService().execute(mailbox);
            }
        }
    }
}
