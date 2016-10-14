package io.reactivej;

import com.google.common.base.MoreObjects;

/***
 * @author heartup@gmail.com
 */
public class MessageCannotSend extends ReactiveException {
    private final Envelope envelope;

    public MessageCannotSend(Envelope envelope, Exception e) {
        super(e);
        this.envelope = envelope;
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("envelope", envelope)
                .toString();
    }
}
