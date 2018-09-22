package io.reactivej;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/***
 * @author heartup@gmail.com
 */
public interface ReactiveSystem {

	String componentSplitter = "/";
	String CONFIG_CHILDREN = "children";
	String CONFIG_SINGLETON = "singleton";
	String CONFIG_DISPATCHER = "dispatcher";
	enum DispatcherType {
		global, self
	}

	String CONFIG_JOURNAL = "journal";
	enum JournalType {
		shared, local
	}

	String CONFIG_CLASS = "class";
	String CONFIG_PARAMS = "params";

	String STORE_NAME = "store";
	String JOURNAL_NAME = "journal";
	String STORE_PATH = componentSplitter + STORE_NAME;
	String JOURNAL_PATH = componentSplitter + JOURNAL_NAME;

	String SHARED_STORE_NAME = "sharedStore";
	String SHARED_JOURNAL_NAME = "sharedJournal";
	String SHARED_STORE_PATH = componentSplitter + SHARED_STORE_NAME;
	String SHARED_JOURNAL_PATH = componentSplitter + SHARED_JOURNAL_NAME;

	void init();

	String getHost();

	int getPort();

	Map<String, Object> getComponentConfig(String path);

	ReactiveRef getRoot();

	String getReactiveClusterId();

	void createClusterSingleton(String componentName);

	void createClusterSingleton(String componentName, boolean useGlobalDispatcher, String className, Object... params);

	ReactiveRef createReactiveComponent(String componentName);

	ReactiveRef createReactiveComponent(ReactiveRef parent, String componentName);

	ReactiveRef createReactiveComponent(String componentName, boolean useGlobalDispatcher, String className, Object... params);

	ReactiveRef createReactiveComponent(String componentName, boolean useGlobalDispatcher, boolean useSharedJournal, String className, Object... params);

	ReactiveRef createReactiveComponent(ReactiveRef parent, String componentName, boolean useGlobalDispatcher, String className, Object... params);

	ReactiveRef createReactiveComponent(ReactiveRef parent, String componentName, boolean useGlobalDispatcher, boolean useSharedJournal,
										String className, Object... params);

	Scheduler getScheduler();

	Dispatcher getDispatcher();

	void sendMessage(ReactiveRef receiver, Serializable message, ReactiveRef sender);

	boolean supportSingleton();

	ReactiveRef findSingleton(String singletonPath);

	String getSingletonLocation(String singletonPath);

	ReactiveRef findComponent(String host, int port, String path);

	ReactiveRef findComponent(String path);

	void setRootComponentClass(String rootComponentClass);

	AbstractTransporter getTransporter();

	ClassLoader getSystemClassLoader();

	void setSystemClassLoader(ClassLoader cl);
}
