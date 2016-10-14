package io.reactivej.persist;

import com.google.common.base.MoreObjects;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class RecoveryComplete implements Serializable {

    private final Serializable persistentId;

    public RecoveryComplete(Serializable persistentId) {
        this.persistentId = persistentId;
    }

    public Serializable getPersistentId() {
        return persistentId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("persistentId", persistentId)
                .toString();
    }
}
