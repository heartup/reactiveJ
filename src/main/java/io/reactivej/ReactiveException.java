package io.reactivej;

/***
 * ReactiveException 是Reactive系统抛出的运行时异常信息
 */
public class ReactiveException extends RuntimeException {

    public ReactiveException(String message) {
        super(message);
    }

    public ReactiveException(Throwable cause) {
        super(cause);
    }

}
