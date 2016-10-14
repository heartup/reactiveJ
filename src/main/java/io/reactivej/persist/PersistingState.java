package io.reactivej.persist;

import io.reactivej.SystemMessage;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class PersistingState extends PersistentComponentState {
    public PersistingState(PersistentReactiveComponent component) {
        super(component);
    }

    @Override
    public void onMessage(Serializable msg) throws Exception {
        if (msg instanceof WriteMessagesSuccessful) {
            onWriteMessagesSuccessful((WriteMessagesSuccessful) msg);
        }
        else if (msg instanceof SnapshotSuccessful) {
            onSnapshotSuccessful((SnapshotSuccessful) msg);
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

    protected void onSnapshotSuccessful(SnapshotSuccessful msg) throws Exception {
        PersistentReactiveComponent.MessageAndHandler head = getComponent().getPendingSnapshotCallbacks().remove(0);
        try {
            head.getHandler().apply(head.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            change2ProcessingStateIfNoPendingCallbacks(true);
            throw e;
        }

        change2ProcessingStateIfNoPendingCallbacks(false);
    }

    protected void onWriteMessagesSuccessful(WriteMessagesSuccessful msg) throws Exception {
        PersistentReactiveComponent.MessageAndHandler head = getComponent().getPendingInvocations().remove(0);
        try {
            head.getHandler().apply(head.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            change2ProcessingStateIfNoPendingCallbacks(true);
            throw e;
        }

        change2ProcessingStateIfNoPendingCallbacks(false);
    }

    private void change2ProcessingStateIfNoPendingCallbacks(boolean errorOccured) {
        if (getComponent().getPendingSnapshotCallbacks().size() == 0 &&
                getComponent().getPendingInvocations().size() == 0) {
            getComponent().setCurrentState(getComponent().getProcessingCommandState());

            if (errorOccured)
                getComponent().unstashAll();
            else
                getComponent().unstash();
        }
    }

}
