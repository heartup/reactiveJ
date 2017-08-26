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

    private final Jedis jedis;

    public RedisStore(Jedis jedis) {
        this.jedis = jedis;
    }

    public Jedis getJedis() {
        return jedis;
    }


    @Override
    protected void snapshot(Snapshot snapshot) {
        getJedis().set(SafeEncoder.encode(getRedisStoreKeyByPersistentId(snapshot.getPersistentId().toString())),
                SerializationUtils.serialize(snapshot));
    }

    @Override
    protected Snapshot recover(Serializable persistentId) {
        byte[] bytes = getJedis().get(SafeEncoder.encode(getRedisStoreKeyByPersistentId(persistentId.toString())));
        return bytes == null ? null : (Snapshot) SerializationUtils.deserialize(bytes);
    }

    private String getRedisStoreKeyByPersistentId(String persistentId) {
        return "reactivej-snapshot-" + getContext().getSystem().getReactiveClusterId() + ReactiveSystem.componentSplitter + persistentId;
    }
}
