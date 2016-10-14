package io.reactivej.persist;

import com.google.common.base.MoreObjects;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class SnapshotOffer implements Serializable {

    private final Serializable persistentId;
    private final long journalSequence;
    private final Serializable snapshot;

    public SnapshotOffer(Serializable persistentId, long journalSequence, Serializable snapshot) {
        this.persistentId = persistentId;
        this.journalSequence = journalSequence;
        this.snapshot = snapshot;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("persistentId", persistentId)
                .add("journalSequence", journalSequence)
                .add("snapshot", snapshot)
                .toString();
    }
}
