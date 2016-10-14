package io.reactivej.persist;

import io.reactivej.ReactiveSystem;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.util.SafeEncoder;

import java.io.*;
import java.util.List;

/***
 * @author heartup@gmail.com
 */
public class RedisJournal extends AbstractJournal {

    private static Logger logger = LoggerFactory.getLogger(RedisJournal.class);

    private final Jedis cacheClient;

    public RedisJournal(Jedis cacheClient) {
        this.cacheClient = cacheClient;
    }

    public Jedis getCacheClient() {
        return cacheClient;
    }

    @Override
    protected void atomicWrite(AtomicWrite write) {
        if (logger.isDebugEnabled())
            logger.debug("redis store [" + write.toString() + "]");

        getCacheClient().rpush(SafeEncoder.encode(getRedisJournalKeyByPersistentId(write.getPersistentId().toString())),
                SerializationUtils.serialize(write));
    }

    @Override
    protected long journalLength(Serializable persistentId) {
        return getCacheClient().llen(SafeEncoder.encode(getRedisJournalKeyByPersistentId(persistentId.toString())));
    }

    protected void replayRange(Serializable persistentId, long from, long end) {
        List<byte[]> list = getCacheClient().lrange(SafeEncoder.encode(getRedisJournalKeyByPersistentId(persistentId.toString())), from, end);
        for (byte[] ele : list) {
            replayAtomicWrite((AtomicWrite) SerializationUtils.deserialize(ele));
        }
    }

    private String getRedisJournalKeyByPersistentId(String persistentId) {
        return "reactivej-journal-" + getContext().getSystem().getReactiveClusterId() + ReactiveSystem.componentSplitter + persistentId;
    }
}
