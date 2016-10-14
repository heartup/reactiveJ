package io.reactivej.persist;

import io.reactivej.ReactiveSystem;
import org.apache.commons.lang3.SerializationUtils;
import redis.clients.jedis.Jedis;
import redis.clients.util.SafeEncoder;

import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class RedisStore extends AbstractStore {

    private final Jedis cacheClient;

    public RedisStore(Jedis cacheClient) {
        this.cacheClient = cacheClient;
    }

    public Jedis getCacheClient() {
        return cacheClient;
    }

    @Override
    protected void snapshot(Snapshot snapshot) {
        getCacheClient().set(SafeEncoder.encode(getRedisStoreKeyByPersistentId(snapshot.getPersistentId().toString())), SerializationUtils.serialize(snapshot));
    }

    @Override
    protected Snapshot recover(Serializable persistentId) {
        return (Snapshot) SerializationUtils.deserialize(getCacheClient().get(SafeEncoder.encode(getRedisStoreKeyByPersistentId(persistentId.toString()))));
    }

    private String getRedisStoreKeyByPersistentId(String persistentId) {
        return "reactivej-snapshot-" + getContext().getSystem().getReactiveClusterId() + ReactiveSystem.componentSplitter + persistentId;
    }
}
