package io.reactivej.persist;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class NullJournal extends AbstractJournal {

    @Override
    protected long journalLength(Serializable persistentId) {
        return 0;
    }

    @Override
    protected void replayRange(Serializable persistentId, long from, long end) {

    }

    @Override
    protected void atomicWrite(AtomicWrite write) {
        System.out.println("持久化: " + write.toString());
    }
}
