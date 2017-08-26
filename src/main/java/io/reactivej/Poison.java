package io.reactivej;

import com.google.common.base.MoreObjects;

public class Poison extends SystemMessage {

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .toString();
    }
}
