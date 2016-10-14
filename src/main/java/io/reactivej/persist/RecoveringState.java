package io.reactivej.persist;

import io.reactivej.SystemMessage;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class RecoveringState extends PersistentComponentState {
    public RecoveringState(PersistentReactiveComponent component) {
        super(component);
    }

    @Override
    public void onMessage(Serializable msg) throws Exception {
        if (msg instanceof ReplayedMessage) {
            onReplayedMessage((ReplayedMessage) msg);
        }
        else if (msg instanceof SnapshotOffer) {
            onSnapshotOffer((SnapshotOffer) msg);
        }
        else if (msg instanceof RecoveryComplete) {
            onRecoveryComplete((RecoveryComplete) msg);
        }
        else {
            if (msg instanceof SystemMessage) {
                getComponent().getBehavior().onMessage(msg);
            }
            else {
                getComponent().stash();
            }
        }
    }

    private void onRecoveryComplete(RecoveryComplete msg) throws Exception {
        getComponent().getRecoverBehavior().onMessage(msg);
        getComponent().setCurrentState(getComponent().getProcessingCommandState());
        getComponent().become(getComponent().getDefaultBehavior());
        getComponent().unstashAll();
    }

    private void onSnapshotOffer(SnapshotOffer msg) throws Exception {
        getComponent().setPersistSequence(msg.getJournalSequence());
        if (msg.getSnapshot() != null) {
            getComponent().getRecoverBehavior().onMessage(msg);
        }
        getComponent().getJournal().tell(new ReplayMessages(msg.getPersistentId(), msg.getJournalSequence()), getComponent().getSelf());
    }

    private void onReplayedMessage(ReplayedMessage msg) throws Exception {
        getComponent().setPersistSequence(msg.getPersistSequence() + 1);
        getComponent().getRecoverBehavior().onMessage(msg.getMessage());
    }

}
