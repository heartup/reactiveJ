package io.reactivej;

/***
 * @author heartup@gmail.com
 */
public class ReactiveException extends RuntimeException {

    public ReactiveException(String message) {
        super(message);
    }

    public ReactiveException(Throwable cause) {
        super(cause);
    }

}
