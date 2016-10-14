package io.reactivej.persist;

import com.google.common.base.MoreObjects;

import java.io.Serializable;
import java.util.List;

/***
 * @author heartup@gmail.com
 */
public class WriteMessages implements Serializable {

    private final List<AtomicWrite> writes;

    public WriteMessages(List<AtomicWrite> writes) {
        this.writes = writes;
    }

    public List<AtomicWrite> getWrites() {
        return writes;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("writes", writes)
                .toString();
    }
}
