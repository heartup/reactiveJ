package io.reactivej.persist;

import com.google.common.base.MoreObjects;
import io.reactivej.ReactiveRef;

import java.io.Serializable;
import java.util.List;

/***
 * @author heartup@gmail.com
 */
public class ReplayedMessage implements Serializable {

    private final Serializable persistentId;
    private final long persistSequence;
    private final Serializable message;

    public ReplayedMessage(Serializable persistentId, long persistSequence, Serializable msg) {
        this.persistentId = persistentId;
        this.persistSequence = persistSequence;
        this.message = msg;
    }

    public Serializable getPersistentId() {
        return persistentId;
    }

    public long getPersistSequence() {
        return persistSequence;
    }

    public Serializable getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("persistentId", persistentId)
                .add("persistSequence", persistSequence)
                .add("messages", message)
                .toString();
    }
}
