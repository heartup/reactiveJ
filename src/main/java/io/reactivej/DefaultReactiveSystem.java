package io.reactivej;

import io.reactivej.persist.RedisJournal;
import io.reactivej.persist.RedisStore;
import redis.clients.jedis.Jedis;

/**
 * @author heartup@gmail.com on 2017/3/15.
 */
public class DefaultReactiveSystem extends ZkCacheReactiveSystem {

    private Jedis cacheClient;

    public Jedis getCacheClient() {
        return cacheClient;
    }

    public void setCacheClient(Jedis cacheClient) {
        this.cacheClient = cacheClient;
    }

    @Override
    public boolean supportSingleton() {
        return true;
    }

    @Override
    public String getSingletonLocation(String singletonPath) {
        String r2mSingletonKey = "reactivej-" + getReactiveClusterId() + componentSplitter + singletonPath;
        String peerId = getCacheClient().get(r2mSingletonKey);
        return peerId;
    }

    @Override
    protected void syncZkSingleton2CacheThread(String singletonName) throws Exception {
        String r2mSingletonKey = "reactivej-" + getReactiveClusterId() + componentSplitter + singletonName;
        getCacheClient().setex(r2mSingletonKey, 3, getPeerId());
        while (true) {
            if (!getCacheClient().exists(r2mSingletonKey)) {
                getCacheClient().setex(r2mSingletonKey, 3, getPeerId());
            }
            else {
                getCacheClient().expire(r2mSingletonKey, 3);
            }
            Thread.currentThread().sleep(2000);
        }
    }

    @Override
    protected void createSharedJournalComponent() {
        createReactiveComponent(SHARED_JOURNAL_NAME, true, RedisJournal.class.getName(), getCacheClient());
    }

    @Override
    protected void createSharedStoreComponent() {
        createReactiveComponent(SHARED_STORE_NAME, true, RedisStore.class.getName(), getCacheClient());
    }
}
