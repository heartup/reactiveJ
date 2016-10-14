package io.reactivej;

import com.google.common.base.MoreObjects;

/***
 * @author heartup@gmail.com
 */
public class Failure extends SystemMessage {

    private Envelope envelope;
    private Exception cause;

    public Failure(Envelope envelope, Exception e) {
        this.envelope = envelope;
        this.cause = e;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public Exception getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("envelope", envelope)
                .add("cause", cause)
                .toString();
    }
}
