package io.reactivej.persist;

import com.google.common.base.MoreObjects;
import io.reactivej.ReactiveRef;

import java.io.Serializable;
import java.util.Date;

/***
 * @author heartup@gmail.com
 */
public class Snapshot implements Serializable {

    private final Serializable persistentId;
    private final long journalSequence;
    private final Date timestamp = new Date();
    private final Serializable snapshot;

    private final ReactiveRef sender;

    public Snapshot(Serializable persistentId, long journalSequence, Serializable snapshot, ReactiveRef sender) {
        this.persistentId = persistentId;
        this.journalSequence = journalSequence;
        this.snapshot = snapshot;
        this.sender = sender;
    }

    public Serializable getPersistentId() {
        return persistentId;
    }

    public long getJournalSequence() {
        return journalSequence;
    }

    public Serializable getSnapshot() {
        return snapshot;
    }

    public ReactiveRef getSender() {
        return sender;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("persistentId", persistentId)
                .add("journalSequence", journalSequence)
                .add("timestamp", timestamp)
                .add("snapshot", snapshot)
                .add("sender", sender)
                .toString();
    }
}
