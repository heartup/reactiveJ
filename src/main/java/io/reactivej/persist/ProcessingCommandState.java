package io.reactivej.persist;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class ProcessingCommandState extends PersistentComponentState {
    public ProcessingCommandState(PersistentReactiveComponent component) {
        super(component);
    }

    @Override
    public void onMessage(Serializable msg) throws Exception {
        getComponent().getBehavior().onMessage(msg);
        getComponent().flushWriteEvents();
        getComponent().flushSnapshots();
        if (getComponent().getPendingInvocations().size() > 0 ||
                getComponent().getPendingSnapshotCallbacks().size() > 0) {
            getComponent().setCurrentState(getComponent().getPersistingState());
        }
        else {
            getComponent().unstash();
        }
    }
}
