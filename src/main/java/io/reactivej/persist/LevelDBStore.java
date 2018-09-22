package io.reactivej.persist;

import com.google.common.primitives.Longs;
import io.reactivej.ReactiveException;
import org.apache.commons.lang3.SerializationUtils;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by heartup@gmail.com on 8/16/16.
 */
public class LevelDBStore extends AbstractStore {

    public static String STORE_FOLDER = System.getProperty("user.home") + File.separator + "dcf" + File.separator + "snapshot" + File.separator;

    @Override
    protected void snapshot(Snapshot snapshot) {
        Options options = new Options();
        options.createIfMissing(true);
        DB db = null;
        try {
            db = Iq80DBFactory.factory.open(new File(STORE_FOLDER + snapshot.getPersistentId().toString()), options);
            db.put(new byte[]{0}, SerializationUtils.serialize(snapshot));
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
    protected Snapshot recover(Serializable persistentId) {
        File file = new File(STORE_FOLDER + persistentId.toString());
        if (!file.exists()) return null;

        Options options = new Options();
        DB db = null;
        try {
            db = Iq80DBFactory.factory.open(new File(STORE_FOLDER + persistentId.toString()), options);
            return (Snapshot) SerializationUtils.deserialize(db.get(new byte[]{0}));
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
