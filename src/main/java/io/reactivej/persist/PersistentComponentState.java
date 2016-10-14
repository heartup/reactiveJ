package io.reactivej.persist;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public abstract class PersistentComponentState {

    private final PersistentReactiveComponent component;

    public PersistentComponentState(PersistentReactiveComponent component) {
        this.component = component;
    }

    public PersistentReactiveComponent getComponent() {
        return component;
    }

    public abstract void onMessage(Serializable msg) throws Exception;
}
