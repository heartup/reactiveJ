package io.reactivej.persist;

import io.reactivej.AbstractComponentBehavior;
import io.reactivej.ReactiveComponent;
import io.reactivej.ReactiveRef;

import java.io.Serializable;
import java.util.List;

/***
 * @author heartup@gmail.com
 */
public abstract class AbstractJournal extends ReactiveComponent {

    private AbstractComponentBehavior defaultBehavior = new AbstractComponentBehavior(this) {
        @Override
        public void onMessage(Serializable msg) throws Exception {
            if (msg instanceof WriteMessages) {
                onWriteMessages((WriteMessages) msg);
            }
            else if (msg instanceof ReplayMessages) {
                onReplayMessages((ReplayMessages) msg);
            }
        }
    };

    protected void onReplayMessages(ReplayMessages msg) {
        replayMessages(msg.getPersistentId(), msg.getJournalSequence());
        getSender().tell(new RecoveryComplete(msg.getPersistentId()), getSelf());
    }

    protected void replayMessages(Serializable persistentId, long startSequence) {
        long len = journalLength(persistentId);
        if (len - 1 > startSequence) {
            replayRange(persistentId, startSequence, len - 1);
        }
    }

    protected abstract long journalLength(Serializable persistentId);

    protected abstract void replayRange(Serializable persistentId, long from, long end);

    protected void replayAtomicWrite(AtomicWrite write) {
        for (Serializable msg : write.getMessages()) {
            ReplayedMessage replay = new ReplayedMessage(write.getPersistentId(), write.getPersistSequence(), msg);
            getSender().tell(replay, getSelf());
        }
    }

    @Override
    public AbstractComponentBehavior getDefaultBehavior() {
        return defaultBehavior;
    }

    protected void onWriteMessages(WriteMessages msg) {
        List<AtomicWrite> writes = msg.getWrites();
        for (AtomicWrite write : writes) {
            atomicWrite(write);

            ReactiveRef sender = null;
            if (write.getSender() != null) {
                sender = getContext().findComponent(write.getSender().getHost(), write.getSender().getPort(), write.getSender().getPath());
            }

            for (Serializable e : write.getMessages()) {
                getSender().tell(new WriteMessagesSuccessful(write.getPersistentId(), write.getPersistSequence()), sender);
            }
        }
    }

    protected abstract void atomicWrite(AtomicWrite write);
}
