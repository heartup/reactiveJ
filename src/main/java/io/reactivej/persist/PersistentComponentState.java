package io.reactivej.persist;

import java.io.Serializable;

/**
 * Created by heartup@gmail.com on 8/13/16.
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
