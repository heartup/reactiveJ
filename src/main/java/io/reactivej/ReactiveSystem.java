package io.reactivej;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.reactivej.persist.*;
import io.reactivej.util.HostUtil;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/***
 * @author heartup@gmail.com
 */
public class ReactiveSystem {
	private static Logger logger = LoggerFactory.getLogger(ReactiveSystem.class);

	public static final String componentSplitter = "/";

	public static String STORE_NAME = "store";
	public static String JOURNAL_NAME = "journal";
	public static String STORE_PATH = componentSplitter + STORE_NAME;
	public static String JOURNAL_PATH = componentSplitter + JOURNAL_NAME;

	public static String SHARED_STORE_NAME = "sharedStore";
	public static String SHARED_JOURNAL_NAME = "sharedJournal";
	public static String SHARED_STORE_PATH = componentSplitter + SHARED_STORE_NAME;
	public static String SHARED_JOURNAL_PATH = componentSplitter + SHARED_JOURNAL_NAME;

	private int port;
	
	private String zkAddr;

	private String zkNamespace;

	private String rootComponentClass = RootComponent.class.getName();

	private Map<String, ReactiveRef> roots = new ConcurrentHashMap<>();
	
	private Scheduler scheduler;

	private Dispatcher dispatcher = new Dispatcher(Executors.newCachedThreadPool());
	
	private AbstractTransporter transporter;

	private CuratorFramework curatorClient;

	private Jedis cacheClient;

	private LoadingCache<String, ReactiveRef> clusterSingletonCache;

	public ReactiveSystem(int port, String zkAddr, String zkNamespace) {
		logger.debug("*******************ReactiveSystem port " + port + "************************");
		this.port = port;
		this.zkAddr = zkAddr;
		this.zkNamespace = zkNamespace;
	}
	
	public void init() {
		scheduler = new Scheduler(this);
		transporter = new NettyTransporter(this);
		clusterSingletonCache = CacheBuilder.newBuilder()
				.expireAfterWrite(3, TimeUnit.SECONDS)
				.build(
						new CacheLoader<String, ReactiveRef>() {
							public ReactiveRef load(String key) {
								return findSingletonExpensive(key);
							}
						});

		ExponentialBackoffRetry retryPolicy = new ExponentialBackoffRetry(1000, 3);
		curatorClient = CuratorFrameworkFactory.builder().namespace(zkNamespace).connectString(zkAddr).retryPolicy(retryPolicy).connectionTimeoutMs(5000).sessionTimeoutMs(3000).build();
		curatorClient.start();

		try {
			RootComponent root = initRoot(getHost(), getPort(), getRootComponentClass());
			root.preStart();
		} catch (Exception e) {
			throw new ReactiveException(e);
		}

		createReactiveComponent(JOURNAL_NAME, true, LevelDBJournal.class.getName());
		createReactiveComponent(STORE_NAME, true, LevelDBStore.class.getName());
		if (cacheClient != null) {
			createReactiveComponent(SHARED_JOURNAL_NAME, true, RedisJournal.class.getName(), cacheClient);
			createReactiveComponent(SHARED_STORE_NAME, true, RedisStore.class.getName(), cacheClient);
		}
	}

	public RootComponent initRoot(String host, int port, String rootComponentClass) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
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

	public String getHost() {
		return HostUtil.getSuitLocalAddress();
	}
	
	public int getPort() {
		return port;
	}

	public ReactiveRef getRoot() {
		return roots.get(getPeerId());
	}

	public String getReactiveClusterId() {
		return zkAddr + componentSplitter + zkNamespace;
	}

	public void createClusterSingleton(final String componentPath, final boolean useGlobalDispatcher, final String className, final Object... params) {
		LeaderSelector singletonSelector = new LeaderSelector(curatorClient, ZKPaths.PATH_SEPARATOR + componentPath, new LeaderSelectorListenerAdapter() {

			@Override
			public void takeLeadership(CuratorFramework client) throws Exception {
				createReactiveComponent(componentPath, useGlobalDispatcher, true, className, params);
				String r2mSingletonKey = "reactivej-" + getReactiveClusterId() + componentSplitter + componentPath;
				cacheClient.set(r2mSingletonKey, getPeerId());
				while (true) {
					cacheClient.expire(r2mSingletonKey, 3);
					Thread.currentThread().sleep(2000);
				}
			}
		});
		singletonSelector.setId(getPeerId());

		singletonSelector.autoRequeue();
		singletonSelector.start();
	}

	public ReactiveRef createReactiveComponent(String componentName, boolean useGlobalDispatcher, String className, Object... params) {
		return createReactiveComponent(getRoot(), componentName, useGlobalDispatcher, false, className, params);
	}

	public ReactiveRef createReactiveComponent(String componentName, boolean useGlobalDispatcher, boolean useSharedJournal, String className, Object... params) {
		return createReactiveComponent(getRoot(), componentName, useGlobalDispatcher, useSharedJournal, className, params);
	}

	public ReactiveRef createReactiveComponent(ReactiveRef parent, String componentName, boolean useGlobalDispatcher, String className, Object... params) {
		return createReactiveComponent(parent, componentName, useGlobalDispatcher, false, className, params);
	}

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

	public Scheduler getScheduler() {
		return scheduler;
	}

	public Dispatcher getDispatcher() {
		return dispatcher;
	}

	public boolean isLocal(ReactiveRef ref) {
		return ref.getHost().equals(getHost()) && ref.getPort() == getPort();
	}
	
	public void sendMessage(ReactiveRef receiver, Serializable message, ReactiveRef sender) {
		Envelope envlop = new Envelope(receiver, message, sender);
		if (logger.isDebugEnabled()) {
				logger.debug("send message [" + envlop.toString() + "]");
		}

		if (isLocal(receiver)) {
			Envelope newEnvlop = transportLocally(envlop);
			transporter.receiveMessage(receiver, newEnvlop);
		}
		else {
			transporter.sendMessage(receiver, envlop);
		}
	}

	private Envelope transportLocally(Envelope envlop) {
		return new Envelope(envlop.getReceiver(), (Serializable) SerializationUtils.clone(envlop.getMessage()), envlop.getSender());
	}

	public ReactiveRef getRoot(String peerId) {
		return roots.get(peerId);
	}

	public ReactiveRef findSingleton(String singletonPath) {
		try {
			return clusterSingletonCache.get(singletonPath);
		} catch (ExecutionException e) {
			throw new ReactiveException(e);
		}
	}

	public String getSingletonLocation(String singletonPath) {
		String r2mSingletonKey = "reactivej-" + getReactiveClusterId() + componentSplitter + singletonPath;
		String peerId = cacheClient.get(r2mSingletonKey);
		return peerId;
	}

	private ReactiveRef findSingletonExpensive(String singletonPath) {
		String peerId = getSingletonLocation(singletonPath);
		if (peerId == null)
			return null;

		String[] loc = peerId.split(":");
		return findComponent(loc[0], Integer.parseInt(loc[1]), componentSplitter + singletonPath);
	}

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

	public ReactiveRef findComponent(String path) {
		String peerId = getHost() + ":" + getPort();
		ReactiveRef root = roots.get(peerId);

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

	public Jedis getCacheClient() {
		return cacheClient;
	}

	public void setCacheClient(Jedis cacheClient) {
		this.cacheClient = cacheClient;
	}

	public String getRootComponentClass() {
		return rootComponentClass;
	}

	public void setRootComponentClass(String rootComponentClass) {
		this.rootComponentClass = rootComponentClass;
	}

	public AbstractTransporter getTransporter() {
		return transporter;
	}
}
