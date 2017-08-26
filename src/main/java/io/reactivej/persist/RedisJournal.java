package io.reactivej.persist;

import io.reactivej.ReactiveException;
import io.reactivej.ReactiveSystem;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.List;

/***
 * @author heartup@gmail.com
 */
public class RedisJournal extends AbstractJournal {

    private static Logger logger = LoggerFactory.getLogger(RedisJournal.class);

    private static String KEY_ENCODING = "utf-8";

    private final Jedis jedis;

    public RedisJournal(Jedis jedis) {
        this.jedis = jedis;
    }

    public Jedis getJedis() {
        return jedis;
    }

    @Override
    protected void atomicWrite(AtomicWrite write) {
        if (logger.isDebugEnabled())
            logger.debug("R2M事件存储[" + write.toString() + "]");

        try {
            getJedis().rpush(getRedisJournalKeyByPersistentId(write.getPersistentId().toString()).getBytes(KEY_ENCODING),
                    SerializationUtils.serialize(write));
        } catch (IOException e) {
            throw new ReactiveException(e);
        }
    }

    @Override
    protected long journalLength(Serializable persistentId) {
        try {
            return getJedis().llen(getRedisJournalKeyByPersistentId(persistentId.toString()).getBytes(KEY_ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new ReactiveException(e);
        }
    }

    protected void replayRange(Serializable persistentId, long from, long end) {
        try {
            List<byte[]> list = getJedis().lrange(getRedisJournalKeyByPersistentId(persistentId.toString()).getBytes(KEY_ENCODING), from, end);
            for (byte[] ele : list) {
                replayAtomicWrite((AtomicWrite) SerializationUtils.deserialize(ele));
            }
        } catch (IOException e) {
            throw new ReactiveException(e);
        }
    }

    private String getRedisJournalKeyByPersistentId(String persistentId) {
        return "reactivej-journal-" + getContext().getSystem().getReactiveClusterId() + ReactiveSystem.componentSplitter + persistentId;
    }
}
