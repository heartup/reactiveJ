package io.reactivej.persist;

import io.reactivej.AbstractComponentBehavior;
import io.reactivej.ReactiveComponent;
import io.reactivej.ReactiveRef;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public abstract class AbstractStore extends ReactiveComponent {

    private AbstractComponentBehavior defaultBehavior = new AbstractComponentBehavior(this) {
        @Override
        public void onMessage(Serializable msg) throws Exception {
            if (msg instanceof Snapshot) {
                onTakeSnapshot((Snapshot) msg);
            }
            else if (msg instanceof StartRecovery) {
                onStartRecovery((StartRecovery) msg);
            }
        }
    };

    private void onStartRecovery(StartRecovery msg) throws Exception {
        Snapshot snapshot = recover(msg.getPersistentId());
        if (snapshot != null) {
            getSender().tell(new SnapshotOffer(snapshot.getPersistentId(), snapshot.getJournalSequence(), snapshot.getSnapshot()), getSelf());
        }
        else {
            getSender().tell(new SnapshotOffer(msg.getPersistentId(), 0L, null), getSelf());
        }
    }

    @Override
    public AbstractComponentBehavior getDefaultBehavior() {
        return defaultBehavior;
    }

    protected void onTakeSnapshot(Snapshot msg) {
        snapshot(msg);

        ReactiveRef sender = null;
        if (msg.getSender() != null) {
            sender = getContext().findComponent(msg.getSender().getHost(), msg.getSender().getPort(), msg.getSender().getPath());
        }

        getSender().tell(new SnapshotSuccessful(msg.getPersistentId(), msg.getJournalSequence()), sender);
    }

    protected abstract void snapshot(Snapshot snapshot);

    protected abstract Snapshot recover(Serializable persistentId);
}
