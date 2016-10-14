package io.reactivej.persist;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 * @param <T>
 */
public interface Procedure<T> {

    public void apply(T param) throws Exception;

}
