package io.reactivej;

import io.reactivej.persist.RedisJournal;
import io.reactivej.persist.RedisStore;
import redis.clients.jedis.Jedis;

/**
 * @author heartup@gmail.com on 2017/3/15.
 */
public class ZkRedisReactiveSystem extends ZkCacheReactiveSystem {

    private Jedis jedis;

    public Jedis getJedis() {
        return jedis;
    }

    public void setJedis(Jedis jedis) {
        this.jedis = jedis;
    }

    @Override
    public boolean supportSingleton() {
        return true;
    }

    @Override
    public String getSingletonLocation(String singletonPath) {
        String singletonKey = "reactivej-" + getReactiveClusterId() + componentSplitter + singletonPath;
        String peerId = getJedis().get(singletonKey);
        return peerId;
    }

    @Override
    protected void syncZkSingleton2CacheThread(String singletonName) throws Exception {
        String singletonKey = "reactivej-" + getReactiveClusterId() + componentSplitter + singletonName;
        getJedis().setex(singletonKey, 3, getPeerId());
        while (true) {
            if (!getJedis().exists(singletonKey)) {
                getJedis().setex(singletonKey, 3, getPeerId());
            }
            else {
                getJedis().expire(singletonKey, 3);
            }
            Thread.currentThread().sleep(2000);
        }
    }

    @Override
    protected void createSharedJournalComponent() {
        createReactiveComponent(SHARED_JOURNAL_NAME, true, RedisJournal.class.getName(), getJedis());
    }

    @Override
    protected void createSharedStoreComponent() {
        createReactiveComponent(SHARED_STORE_NAME, true, RedisStore.class.getName(), getJedis());
    }
}
