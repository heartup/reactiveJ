package io.reactivej.persist;

import com.google.common.base.MoreObjects;
import io.reactivej.ReactiveRef;

import java.io.Serializable;
import java.util.List;

/***
 * @author heartup@gmail.com
 */
public class AtomicWrite implements Serializable {

    private final Serializable persistentId;
    private final long persistSequence;
    private final List<Serializable> messages;
    private final ReactiveRef sender;

    public AtomicWrite(Serializable persistentId, long persistSequence, List<Serializable> batchMsg, ReactiveRef sender) {
        this.persistentId = persistentId;
        this.persistSequence = persistSequence;
        this.messages = batchMsg;
        this.sender = sender;
    }

    public List<Serializable> getMessages() {
        return messages;
    }

    public Serializable getPersistentId() {
        return persistentId;
    }

    public long getPersistSequence() {
        return persistSequence;
    }

    public ReactiveRef getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("persistentId", persistentId)
                .add("persistSequence", persistSequence)
                .add("messages", messages)
                .add("sender", sender)
                .toString();
    }
}
