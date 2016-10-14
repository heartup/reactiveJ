package io.reactivej.persist;

import com.google.common.base.MoreObjects;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class ReplayMessages implements Serializable {

    private final Serializable persistentId;
    private final long journalSequence;  // start seq, include

    public ReplayMessages(Serializable persistentId, long journalSequence) {
        this.persistentId = persistentId;
        this.journalSequence = journalSequence;
    }

    public Serializable getPersistentId() {
        return persistentId;
    }

    public long getJournalSequence() {
        return journalSequence;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("persistentId", persistentId)
                .add("journalSequence", journalSequence)
                .toString();
    }
}
