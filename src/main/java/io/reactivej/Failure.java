package io.reactivej;

import com.google.common.base.MoreObjects;

public class Failure extends SystemMessage {

    private Envelope envelope;
    private Throwable cause;

    public Failure(Envelope envelope, Throwable e) {
        this.envelope = envelope;
        this.cause = e;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public Throwable getCause() {
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
