package io.reactivej;

import io.reactivej.persist.LevelDBJournal;
import io.reactivej.persist.LevelDBStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class ConfigReactiveSystem extends AbstractReactiveSystem {
	private static Logger logger = LoggerFactory.getLogger(ConfigReactiveSystem.class);

	private Scheduler scheduler;

	private Dispatcher dispatcher;

	private AbstractTransporter transporter;

	private Map<String, String> singletonLocs = new HashMap<>();
	private Map<String, ReactiveRef> singletonCache = new HashMap<>();

	private Map<String, Object> clusterConfig;

	public static final String CONFIG_CLUSTER_NAME = "name";
	public static final String CONFIG_SINGLETONS = "singletons";
	public static final String CONFIG_SINGLETON_HOST = "host";
	public static final String CONFIG_SINGLETON_PORT = "port";

	@Override
	public boolean supportSingleton() {
		return true;
	}

	@Override
	public void init() {
		dispatcher = new Dispatcher(Executors.newCachedThreadPool());
		scheduler = new Scheduler(this);
		transporter = new NettyTransporter(this);

		try {
			RootComponent root = initRoot(getHost(), getPort(), getRootComponentClass());
			root.preStart();
		} catch (Exception e) {
			throw new ReactiveException(e);
		}

		createReactiveComponent(JOURNAL_NAME, true, LevelDBJournal.class.getName());
		createReactiveComponent(STORE_NAME, true, LevelDBStore.class.getName());
		if (supportSingleton()) {
			createSharedStoreComponent();
			createSharedJournalComponent();
		}
	}

	public Map<String, Object> getClusterConfig() {
		return clusterConfig;
	}

	public void setClusterConfig(Map<String, Object> clusterConfig) {
		this.clusterConfig = clusterConfig;
	}

	@Override
	public String getReactiveClusterId() {
		return (String) getClusterConfig().get(CONFIG_CLUSTER_NAME);
	}

	@Override
	public void createClusterSingleton(final String componentName, final boolean useGlobalDispatcher, final String className, final Object... params) {
		String loc = getSingletonLocation(componentName);
		if (loc == null)
			return;

		if (loc.equals(getPeerId())) {
			logger.debug("开始创建Singleton[{}]", componentName);
			createReactiveComponent(componentName, useGlobalDispatcher, true, className, params);
		}
	}

	@Override
	public Scheduler getScheduler() {
		return scheduler;
	}

	@Override
	public Dispatcher getDispatcher() {
		return dispatcher;
	}

	@Override
	public ReactiveRef findSingleton(String singletonPath) {
		ReactiveRef ref = singletonCache.get(singletonPath);
		if (ref == null) {
			ref = findSingletonExpensive(singletonPath);
			singletonCache.put(singletonPath, ref);
		}

		return ref;
	}

	@Override
	public String getSingletonLocation(String singletonPath) {
		String loc = singletonLocs.get(singletonPath);
		if (loc == null) {
			Map<String, Object> singletons = (Map<String, Object>) getClusterConfig().get(CONFIG_SINGLETONS);
			Map<String, Object> thisConf = (Map<String, Object>) singletons.get(singletonPath);

			String shost = (String) thisConf.get(CONFIG_SINGLETON_HOST);
			shost = shost.trim();
			String sport = (String) thisConf.get(CONFIG_SINGLETON_PORT);
			sport = sport.trim();

			loc = shost + ":" + sport;
			singletonLocs.put(singletonPath, loc);
		}

		return loc;
	}

	private ReactiveRef findSingletonExpensive(String singletonPath) {
		String peerId = getSingletonLocation(singletonPath);
		if (peerId == null)
			return null;

		String[] loc = peerId.split(":");
		return findComponent(loc[0], Integer.parseInt(loc[1]), componentSplitter + singletonPath);
	}

	@Override
	public AbstractTransporter getTransporter() {
		return transporter;
	}

	@Override
	protected void createSharedJournalComponent() {
		createReactiveComponent(SHARED_JOURNAL_NAME, true, LevelDBJournal.class.getName());
	}

	@Override
	protected void createSharedStoreComponent() {
		createReactiveComponent(SHARED_STORE_NAME, true, LevelDBStore.class.getName());
	}
}
