package io.reactivej.persist;


import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class NullStore extends AbstractStore {


    public NullStore() {
    }


    @Override
    protected void snapshot(Snapshot snapshot) {

    }

    @Override
    protected Snapshot recover(Serializable persistentId) {
        return null;
    }
}
