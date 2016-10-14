package io.reactivej.persist;

import com.google.common.base.MoreObjects;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class WriteMessagesSuccessful implements Serializable {

    private final Serializable persistentId;
    private final long journalSequence;

    public WriteMessagesSuccessful(Serializable persistentId, long journalSequence) {
        this.persistentId = persistentId;
        this.journalSequence = journalSequence;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("persistentId", persistentId)
                .add("journalSequence", journalSequence)
                .toString();
    }
}
