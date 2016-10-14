package io.reactivej;

import com.google.common.base.MoreObjects;

import java.io.Serializable;

/**
 * @author heartup@gmail.com
 */
public class ClusterClient extends ReactiveComponent {

    public static class ClusterMessage implements Serializable {
        private String clusterPath;
        private Serializable msg;

        public ClusterMessage(String clusterPath, Serializable msg) {
            this.clusterPath = clusterPath;
            this.msg = msg;
        }

        public String getClusterPath() {
            return clusterPath;
        }

        public Serializable getMsg() {
            return msg;
        }

        public void setClusterPath(String clusterPath) {
            this.clusterPath = clusterPath;
        }

        public void setMsg(Serializable msg) {
            this.msg = msg;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("clusterPath", clusterPath)
                    .add("msg", msg)
                    .toString();
        }
    }

    @Override
    public AbstractComponentBehavior getDefaultBehavior() {
        return defaultBehavior;
    }

    private AbstractComponentBehavior defaultBehavior = new AbstractComponentBehavior(this) {
        @Override
        public void onMessage(Serializable msg) throws Exception {
            if (msg instanceof ClusterMessage) {
                ClusterMessage message = (ClusterMessage) msg;
                String singltonName = message.getClusterPath();
                Serializable theMsg = message.getMsg();
                ReactiveRef ref = getContext().findSingleton(singltonName);
                if (ref == null) {
                    throw new SingletonNonExistException();
                }

                ref.tell(theMsg, getSender());
            }
        }
    };

    public static class SingletonNonExistException extends Exception {}
}
