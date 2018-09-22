package io.reactivej.persist;

import java.io.Serializable;

/**
 * Created by heartup@gmail.com on 4/1/16.
 */
public interface Procedure<T> {

    public void apply(T param) throws Exception;

}
