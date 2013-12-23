package cn.encore.lib.imagecache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap.CompressFormat;
import android.support.v4.util.LruCache;
import cn.encore.lib.BuildConfig;
import cn.encore.lib.utils.Log;


public abstract class AbstractCache<ValT> {

    private static final String TAG = "AbstractCache";

    // Default memory cache size
    private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 1024 * 5; // 5MB

    // Default disk cache size
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB

    // Compression settings when writing images to disk cache
    private static final CompressFormat DEFAULT_COMPRESS_FORMAT = CompressFormat.JPEG;
    private static final int DEFAULT_COMPRESS_QUALITY = 70;

    // Constants to easily toggle various caches
    private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
    private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;
    private static final boolean DEFAULT_CLEAR_DISK_CACHE_ON_START = false;

    public static final String INTENT_NOT_ENOUGH_SPACE = "intent_not_enough_space";

    protected DiskLruCache mDiskCache;
    protected LruCache<String, ValT> mMemoryCache;

    /**
     * Creating a new ImageCache object using the specified parameters.
     * 
     * @param context The context to use
     * @param cacheParams The cache parameters to use to initialize the cache
     */
    public AbstractCache(Context context, CacheParams cacheParams) {
        init(context, cacheParams);
    }

    /**
     * Creating a new ImageCache object using the default parameters.
     * 
     * @param context The context to use
     * @param uniqueName A unique name that will be appended to the cache directory
     */
    public AbstractCache(Context context, String uniqueName) {
        init(context, new CacheParams(uniqueName));
    }

    /**
     * Initialize the cache, providing all parameters.
     * 
     * @param context The context to use
     * @param cacheParams The cache parameters to initialize the cache
     */
    private void init(Context context, CacheParams cacheParams) {
        final File diskCacheDir = DiskLruCache.getDiskCacheDir(context, cacheParams.uniqueName);

        // Set up disk cache
        if (cacheParams.diskCacheEnabled) {
            mDiskCache = DiskLruCache.openCache(diskCacheDir, cacheParams.diskCacheSize);
            if (cacheParams.clearDiskCacheOnStart && mDiskCache != null) {
                mDiskCache.clearCache();
            }
        }

        // Set up memory cache
        if (cacheParams.memoryCacheEnabled) {
            mMemoryCache = new LruCache<String, ValT>(cacheParams.memCacheSize) {
                /**
                 * Measure item size in bytes rather than units which is more practical for a bitmap
                 * cache
                 */
                @Override
                protected int sizeOf(String key, ValT data) {
                    return getSize(data);
                }
            };
        }
    }

    public void addToCache(String key, ValT value) {
    	try {
    		 addToCache(key, value, false);
		} catch (Exception e) {
		}
       
    }
    
    /**
     * @param key The key
     * @param value The value
     * @param override 如果是true,当缓存中已存在key时,覆盖当前值到缓存中.否则不覆盖.
     */
    public void addToCache(String key, ValT value, boolean override) {
        if (key == null || value == null) {
            return;
        }
        
        // Add to memory cache
        if (mMemoryCache != null && (override || mMemoryCache.get(key) == null)) {
            mMemoryCache.put(key, value);
        }
        
        // Add to disk cache
        if (mDiskCache != null && (override || !mDiskCache.containsKey(key))) {
            String file = mDiskCache.createFilePath(key);
            try {
                if (writeToFile(value, file)) {
                    mDiskCache.add(key, file);
                }
            } catch (final FileNotFoundException e) {
                Log.e(TAG, "Error in put: " + e.getMessage());
            } catch (final IOException e) {
                Log.e(TAG, "Error in put: " + e.getMessage());
            }
            
        }
    }

    public ValT getFromCache(String key) {
        ValT value = getFromMemCache(key);
        if (value == null) {
            value = getFromDiskCache(key);
        }
        return value;
    }

    /**
     * Get from memory cache.
     * 
     * @param key Unique identifier for which item to get
     * @return The bitmap if found in cache, null otherwise
     */
    public ValT getFromMemCache(String key) {
        if (mMemoryCache != null) {
            final ValT value = mMemoryCache.get(key);
            if (value != null) {
                    Log.d(TAG, "Memory cache hit");
                return value;
            }
        }
        return null;
    }

    /**
     * Get from disk cache.
     * 
     * @param key Unique identifier for which item to get
     * @return The bitmap if found in cache, null otherwise
     */
    public ValT getFromDiskCache(String key) {
        try {
            if (mDiskCache != null) {
                return readValueFromDisk(mDiskCache.get(key));
            }
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 清除缓存
     * 
     * @param clearDisk 是否清除磁盘内的缓存
     */
    public void clearCaches(boolean clearDisk) {
        mMemoryCache.evictAll();
        if (clearDisk && mDiskCache != null) {
            mDiskCache.clearCache();
        }
    }
    
    public boolean containsKey(String key) {
        if (mMemoryCache.get(key) != null) {
            return true;
        }
        if (mDiskCache != null) {
            return mDiskCache.containsKey(key);
        }
        return false;
    }

    /**
     * A holder class that contains cache parameters.
     */
    public static class CacheParams {
        public String uniqueName;
        public int memCacheSize = DEFAULT_MEM_CACHE_SIZE;
        public int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
        public CompressFormat compressFormat = DEFAULT_COMPRESS_FORMAT;
        public int compressQuality = DEFAULT_COMPRESS_QUALITY;
        public boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;
        public boolean diskCacheEnabled = DEFAULT_DISK_CACHE_ENABLED;
        public boolean clearDiskCacheOnStart = DEFAULT_CLEAR_DISK_CACHE_ON_START;

        public CacheParams(String uniqueName) {
            this.uniqueName = uniqueName;
        }
    }

    protected abstract boolean writeToFile(ValT data, String file) throws IOException, FileNotFoundException;

    protected abstract ValT readValueFromDisk(File file) throws IOException;

    protected abstract int getSize(ValT data);
}
