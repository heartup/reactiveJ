package io.reactivej;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.reactivej.persist.LevelDBJournal;
import io.reactivej.persist.LevelDBStore;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderSelector;
import org.apache.curator.framework.recipes.leader.LeaderSelectorListenerAdapter;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class ZkCacheReactiveSystem extends AbstractReactiveSystem {
	private static Logger logger = LoggerFactory.getLogger(ZkCacheReactiveSystem.class);
	
	/**
	 * zk地址，分布式ReactiveComponent基于zk
	 */
	private String zkServers;

	private String zkNamespace;

	private Scheduler scheduler;

	private Dispatcher dispatcher;
	
	private AbstractTransporter transporter;

	private CuratorFramework curatorClient;

	private LoadingCache<String, ReactiveRef> clusterSingletonCache;

	public ZkCacheReactiveSystem() {
	}

	public String getZkServers() {
		return zkServers;
	}

	public void setZkServers(String zkServers) {
		this.zkServers = zkServers;
	}

	public String getZkNamespace() {
		return zkNamespace;
	}

	public void setZkNamespace(String zkNamespace) {
		this.zkNamespace = zkNamespace;
	}

	@Override
	public void init() {
		dispatcher = new Dispatcher(Executors.newCachedThreadPool());
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
		curatorClient = CuratorFrameworkFactory.builder().namespace(getZkNamespace()).connectString(getZkServers()).retryPolicy(retryPolicy).connectionTimeoutMs(5000).sessionTimeoutMs(3000).build();
		curatorClient.start();

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

	@Override
	public String getReactiveClusterId() {
		return getZkServers() + componentSplitter + getZkNamespace();
	}

	@Override
	public void createClusterSingleton(final String componentName, final boolean useGlobalDispatcher, final String className, final Object... params) {
		LeaderSelector singletonSelector = new LeaderSelector(curatorClient, ZKPaths.PATH_SEPARATOR + componentName, new LeaderSelectorListenerAdapter() {

			@Override
			public void takeLeadership(CuratorFramework client) throws Exception {
				logger.debug("开始创建Singleton[{}]", componentName);
				ReactiveRef leaderRef = createReactiveComponent(componentName, useGlobalDispatcher, true, className, params);
				try {
					syncZkSingleton2CacheThread(componentName);
				}
				catch (Exception e) {
					leaderRef.tell(new Poison(), null);
					throw e;
				}
			}
		});
		singletonSelector.setId(getPeerId());

		singletonSelector.autoRequeue();
		singletonSelector.start();
	}

	protected abstract void syncZkSingleton2CacheThread(String singletonName) throws Exception;

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
		try {
			return clusterSingletonCache.get(singletonPath);
		} catch (ExecutionException e) {
			throw new ReactiveException(e);
		}
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
}
