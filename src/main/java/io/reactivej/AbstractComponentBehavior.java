package io.reactivej;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public abstract class AbstractComponentBehavior {

    private final ReactiveComponent component;

    public AbstractComponentBehavior(ReactiveComponent component) {
        this.component = component;
    }

    public ReactiveComponent getComponent() {
        return component;
    }

    public abstract void onMessage(Serializable msg) throws Exception;
}
