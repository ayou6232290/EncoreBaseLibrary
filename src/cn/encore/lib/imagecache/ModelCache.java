package cn.encore.lib.imagecache;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;

/**
 * Allows caching Model objects using the features provided by {@link AbstractCache}. The key into
 * the cache will be based around the cached object's key, and the object will be able to save and
 * reload itself from the cache.
 * 
 * @author Encore.liang
 * 
 */
public class ModelCache extends AbstractCache<CachedModel> {

    // Counter for all saves to cache. Used to determine if newer object in cache
    private long transactionCount = Long.MIN_VALUE + 1;

    public ModelCache(Context context, String uniqueName) {
        super(context, uniqueName);
    }

    @Override
    protected boolean writeToFile(CachedModel data, String file) throws IOException, FileNotFoundException {
        FileOutputStream ostream = null;
        ObjectOutputStream oos = null;
        try {
            // Write byte data to file
            ostream = new FileOutputStream(file);
            oos = new ObjectOutputStream(ostream);
            oos.writeObject(data);
            return true;
        } finally {
            ImageCacheUtils.closeSilently(oos);
        }
    }

    @Override
    protected CachedModel readValueFromDisk(File file) throws IOException {
        if (file != null) {
            FileInputStream istream = new FileInputStream(file);
            
            // Read file into byte array
            byte[] dataWritten = new byte[(int) file.length()];
            BufferedInputStream bistream = new BufferedInputStream(istream);
            bistream.read(dataWritten);
            bistream.close();
            
            ByteArrayInputStream bais = new ByteArrayInputStream(dataWritten);
            ObjectInputStream ois = new ObjectInputStream(bais);
            try {
                return (CachedModel) ois.readObject();
            } catch (ClassNotFoundException e) {
                throw new IOException(e.getMessage());
            }
        }
        return null;
    }

    @Override
    protected int getSize(CachedModel data) {
        return 1;
    }

    @Override
    public void addToCache(String key, CachedModel value) {
        value.setTransactionId(transactionCount++);
        addToCache(key, value, true);
    }
}
