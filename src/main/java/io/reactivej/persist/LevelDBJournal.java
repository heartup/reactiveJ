package io.reactivej.persist;

import com.google.common.primitives.Longs;
import io.reactivej.ReactiveException;
import org.apache.commons.lang3.SerializationUtils;
import org.iq80.leveldb.*;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/***
 * @author heartup@gmail.com
 */
public class LevelDBJournal extends AbstractJournal {

    public static String JOUNAL_FOLDER = System.getProperty("user.home") + File.separator + "reactivej" + File.separator + "journal" + File.separator;

    private DBComparator comparator = new DBComparator(){
        public int compare(byte[] key1, byte[] key2) {
            return Longs.compare(Longs.fromByteArray(key1), Longs.fromByteArray(key2));
        }
        public String name() {
            return "long";
        }
        public byte[] findShortestSeparator(byte[] start, byte[] limit) {
            return start;
        }
        public byte[] findShortSuccessor(byte[] key) {
            return key;
        }
    };

    protected void replayMessages(Serializable persistentId, long startSequence) {
        Options options = new Options();
        options.comparator(comparator);
        DB db = null;
        try {
            db = Iq80DBFactory.factory.open(new File(JOUNAL_FOLDER + persistentId.toString()), options);
            DBIterator it = db.iterator();
            it.seek(Longs.toByteArray(startSequence));
            while (it.hasNext()) {
                byte[] writeByte = it.peekNext().getValue();
                replayAtomicWrite((AtomicWrite) SerializationUtils.deserialize(writeByte));
                it.next();
            }
        } catch (IOException e) {
            throw new ReactiveException(e);
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (IOException e) {
                    throw new ReactiveException(e);
                }
            }
        }
    }

    @Override
    protected long journalLength(Serializable persistentId) {
        return -1;  // means unknown
    }

    @Override
    protected void replayRange(Serializable persistentId, long from, long end) {
        return;
    }

    @Override
    protected void atomicWrite(AtomicWrite write) {
        Options options = new Options();
        options.createIfMissing(true).comparator(comparator);
        DB db = null;
        try {
            db = Iq80DBFactory.factory.open(new File(JOUNAL_FOLDER + write.getPersistentId().toString()), options);
            db.put(Longs.toByteArray(write.getPersistSequence()), SerializationUtils.serialize(write));
        } catch (IOException e) {
            throw new ReactiveException(e);
        } finally {
            if (db != null) {
                try {
                    db.close();
                } catch (IOException e) {
                    throw new ReactiveException(e);
                }
            }
        }
    }
}
