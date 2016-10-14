package io.reactivej;

import java.util.Map;

/***
 * @author heartup@gmail.com
 */
public interface ReactiveContext {

    public Map<String, ReactiveRef> getChildren();

    public ReactiveRef getChild(String childName);

    public ReactiveRef getParent();

    public ReactiveRef getSelf();

    public ReactiveRef createChild(String childName, boolean useGlobalDispatcher, String className);

    public ReactiveRef createChild(String childName, boolean useGlobalDispatcher, String className, Object... params);

    public ReactiveRef getSender();

    public void setSender(ReactiveRef sender);

    public ReactiveSystem getSystem();

    public ReactiveRef findComponent(String path);

    public ReactiveRef findComponent(String host, int port, String path);

    public ReactiveRef findSingleton(String singletonName);
}
