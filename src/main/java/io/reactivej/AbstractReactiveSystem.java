package io.reactivej;

import io.reactivej.persist.PersistentReactiveComponent;
import com.jdjr.cds.job.common.util.HostUtil;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractReactiveSystem implements ReactiveSystem {
	private static Logger logger = LoggerFactory.getLogger(AbstractReactiveSystem.class);

	private String host;
	/**
	 * 系统对外端口（netty通信端口）
	 */
	private int port;

	private Map<String, Object> config;

	private String rootComponentClass = RootComponent.class.getName(); // 定制化的root组件

	private Map<String, ReactiveRef> roots = new ConcurrentHashMap<>();

	private ClassLoader systemClassLoader;


	@Override
	public String getHost() {
		if (host == null) {
			host = HostUtil.getSuitLocalAddress();
		}
		return host;
	}
	
	@Override
	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
		logger.debug("*******************ReactiveSystem port " + port + "************************");
	}

	public Map<String, Object> getConfig() {
		return config;
	}

	public void setConfig(Map<String, Object> config) {
		this.config = config;
	}

	@Override
	public Map<String, Object> getComponentConfig(String path) {
		String[] pathSegs = path.split(ReactiveSystem.componentSplitter);
		Map<String, Object> map = config;
		for (int i = 1; i < pathSegs.length; i ++) {
			if (map != null && map.get(CONFIG_CHILDREN) != null) {
				map = (Map<String, Object>)((Map<String, Object>) map.get(CONFIG_CHILDREN)).get(pathSegs[i]);
			}
			else {
				map = null;
				break;
			}
		}

		logger.debug("组件" + path + "的配置为" + map);
		return map;
	}

	protected RootComponent initRoot(String host, int port, String rootComponentClass) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
		ReactiveRef root = new ReactiveRef("", host, port);
		RootComponent rootComponent = null;
		if (rootComponentClass != null) {
			Class<RootComponent> rootClass = (Class<RootComponent>) Class.forName(rootComponentClass);
			rootComponent = (RootComponent) ConstructorUtils.invokeConstructor(rootClass, null);
		}
		ReactiveCell rootCell = new ReactiveCell(rootComponent, root, null, this, true);
		root.setCell(rootCell);
		if (rootComponent != null) {
			rootComponent.setContext(rootCell);
		}
		roots.put(host + ":" + port, root);

		return rootComponent;
	}

	public String getPeerId() {
		return getHost() + ":" + getPort();
	}

	@Override
	public ReactiveRef getRoot() {
		return roots.get(getPeerId());
	}

	public ReactiveRef getRoot(String peerId) {
		return roots.get(peerId);
	}

	@Override
	public ReactiveRef createReactiveComponent(String componentName) {
		String compPath = componentSplitter + componentName;
		Map<String, Object> compConfig = getComponentConfig(compPath);
		if (compConfig == null)
			throw new ReactiveException("组件" + componentName + "未配置");

		String singleton = (String) compConfig.get(CONFIG_SINGLETON);
		if (singleton == null)
			throw new ReactiveException("组件" + componentName + "未声明是否是Singleton");

		if (Boolean.parseBoolean(singleton)) {
			createClusterSingleton(componentName);
			return null;
		}
		else {
			return createReactiveComponent(getRoot(), componentName);
		}
	}

	@Override
	public void createClusterSingleton(String componentName) {
		String compPath = componentSplitter + componentName;
		Map<String, Object> compConfig = getComponentConfig(compPath);
		if (compConfig == null)
			throw new ReactiveException("组件" + componentName + "未配置");
		if (compConfig.get(CONFIG_CLASS) == null)
			throw new ReactiveException("组件" + componentName + "未定义实现类");
		if (compConfig.get(CONFIG_DISPATCHER) == null)
			throw new ReactiveException("组件" + componentName + "未定义DISPATCHER");

		boolean useGlobalDispatcher = DispatcherType.valueOf((String)compConfig.get(CONFIG_DISPATCHER)) == DispatcherType.global ? true : false;

		if (compConfig.get(CONFIG_PARAMS) == null) {
			createClusterSingleton(componentName, useGlobalDispatcher, (String) compConfig.get(CONFIG_CLASS));
		}
		else {
			createClusterSingleton(componentName, useGlobalDispatcher, (String) compConfig.get(CONFIG_CLASS), ((List) compConfig.get(CONFIG_PARAMS)).toArray());
		}
	}

	@Override
	public ReactiveRef createReactiveComponent(ReactiveRef parent, String componentName) {
		String compPath = parent.getPath() + componentSplitter + componentName;
		Map<String, Object> compConfig = getComponentConfig(compPath);
		if (compConfig == null)
			throw new ReactiveException("组件" + componentName + "未配置");
		if (compConfig.get(CONFIG_CLASS) == null)
			throw new ReactiveException("组件" + componentName + "未定义实现类");
		if (compConfig.get(CONFIG_DISPATCHER) == null)
			throw new ReactiveException("组件" + componentName + "未定义DISPATCHER");
		if (compConfig.get(CONFIG_JOURNAL) == null)
			throw new ReactiveException("组件" + componentName + "未定义JOURNAL");

		boolean useGlobalDispatcher = DispatcherType.valueOf((String)compConfig.get(CONFIG_DISPATCHER)) == DispatcherType.global ? true : false;
		boolean useSharedJournal = JournalType.valueOf((String)compConfig.get(CONFIG_JOURNAL)) == JournalType.shared ? true : false;

		if (compConfig.get(CONFIG_PARAMS) == null) {
			return createReactiveComponent(parent, componentName, useGlobalDispatcher, useSharedJournal, (String) compConfig.get(CONFIG_CLASS));
		}
		else {
			return createReactiveComponent(parent, componentName, useGlobalDispatcher, useSharedJournal, (String) compConfig.get(CONFIG_CLASS), ((List) compConfig.get(CONFIG_PARAMS)).toArray());
		}
	}

	@Override
	public ReactiveRef createReactiveComponent(String componentName, boolean useGlobalDispatcher, String className, Object... params) {
		return createReactiveComponent(getRoot(), componentName, useGlobalDispatcher, false, className, params);
	}

	@Override
	public ReactiveRef createReactiveComponent(String componentName, boolean useGlobalDispatcher, boolean useSharedJournal, String className, Object... params) {
		return createReactiveComponent(getRoot(), componentName, useGlobalDispatcher, useSharedJournal, className, params);
	}

	@Override
	public ReactiveRef createReactiveComponent(ReactiveRef parent, String componentName, boolean useGlobalDispatcher, String className, Object... params) {
		return createReactiveComponent(parent, componentName, useGlobalDispatcher, false, className, params);
	}

	@Override
	public ReactiveRef createReactiveComponent(ReactiveRef parent, String componentName, boolean useGlobalDispatcher, boolean useSharedJournal,
											   String className, Object... params) {
		Class<ReactiveComponent> compClass;
		try {
			ReactiveRef ref = new ReactiveRef(parent.getPath() + componentSplitter + componentName, parent.getHost(), parent.getPort());

			ReactiveComponent comp = null;

			if (className != null) {
				compClass = (Class<ReactiveComponent>) Class.forName(className);
				comp = (ReactiveComponent) ConstructorUtils.invokeConstructor(compClass, params);
			}

			ReactiveCell cell = new ReactiveCell(comp, ref, parent, this, useGlobalDispatcher);

			if (comp != null)
				comp.setContext(cell);

			ref.setCell(cell);
			parent.getCell().getChildren().put(componentName, ref);

			if (comp != null) {
				if (comp instanceof PersistentReactiveComponent) {
					if (useSharedJournal) {
						((PersistentReactiveComponent) comp).setStore(findComponent(SHARED_STORE_PATH));
						((PersistentReactiveComponent) comp).setJournal(findComponent(SHARED_JOURNAL_PATH));
					}
					else {
						((PersistentReactiveComponent) comp).setStore(findComponent(STORE_PATH));
						((PersistentReactiveComponent) comp).setJournal(findComponent(JOURNAL_PATH));
					}
				}
				comp.preStart();
			}

			return ref;
		} catch (ClassNotFoundException | InvocationTargetException | InstantiationException | NoSuchMethodException | IllegalAccessException e) {
			throw new ReactiveException(e);
		}
	}

	public boolean isLocal(ReactiveRef ref) {
		return ref.getHost().equals(getHost()) && ref.getPort() == getPort();
	}

	@Override
	public void sendMessage(ReactiveRef receiver, Serializable message, ReactiveRef sender) {
		Envelope envlop = new Envelope(receiver, message, sender);
//		if (logger.isDebugEnabled()) {
		if (!(envlop.getMessage() instanceof SystemMessage)
				&& !(envlop.getMessage() instanceof ClusterClient.ClusterMessage)) {
			logger.info("发送消息[" + envlop.toString() + "]");
		}
		else {
			logger.debug("发送消息[" + envlop.toString() + "]");
		}
//		}

		if (isLocal(receiver)) {
			Envelope newEnvelop = transportLocally(envlop);
			getTransporter().receiveMessage(receiver, newEnvelop);
		}
		else {
			getTransporter().sendMessage(receiver, envlop);
		}
	}

	private Envelope transportLocally(Envelope envlop) {
		return new Envelope(envlop.getReceiver(), (Serializable) SerializationUtils.clone(envlop.getMessage()), envlop.getSender(), envlop.getFromChannel());
	}

	@Override
	public ReactiveRef findComponent(String host, int port, String path) {
		String peerId = host + ":" + port;
		ReactiveRef root = roots.get(peerId);
		if (root == null) {
			try {
				initRoot(host, port, null);
			} catch (Exception e) {
				throw new ReactiveException(e);
			}
			root = roots.get(peerId);
		}

		String[] pathSegs = path.split(componentSplitter);
		ReactiveCell parent = root.getCell();
		for (int i = 1; i < pathSegs.length; i++) {
			ReactiveRef pRef = parent.getChild(pathSegs[i]);
			if (pRef == null) {
				pRef = createReactiveComponent(parent.getSelf(), pathSegs[i], true, false, null);
			}
			parent = pRef.getCell();
		}

		return parent.getSelf();
	}

	@Override
	public ReactiveRef findComponent(String path) {
		return findComponent(getHost(), getPort(), path);
	}

	public String getRootComponentClass() {
		return rootComponentClass;
	}

	@Override
	public void setRootComponentClass(String rootComponentClass) {
		this.rootComponentClass = rootComponentClass;
	}

	@Override
	public ClassLoader getSystemClassLoader() {
		return systemClassLoader;
	}

	@Override
	public void setSystemClassLoader(ClassLoader systemClassLoader) {
		this.systemClassLoader = systemClassLoader;
	}

	protected abstract void createSharedStoreComponent();

	protected abstract void createSharedJournalComponent();
}
