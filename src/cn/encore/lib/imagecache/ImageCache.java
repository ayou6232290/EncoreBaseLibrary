package cn.encore.lib.imagecache;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.support.v4.app.FragmentActivity;
import cn.encore.lib.utils.Log;

public class ImageCache extends AbstractCache<Bitmap> {
    private static final String TAG = "ImageCache";
    
    public ImageCache(Context context, String uniqueName) {
        super(context, uniqueName);
    }

    public ImageCache(Context context, CacheParams cacheParams) {
        super(context, cacheParams);
        setCompressParams(cacheParams.compressFormat, cacheParams.compressQuality);
    }

    private CompressFormat mCompressFormat = CompressFormat.JPEG;
    private int mCompressQuality = 70;

    /**
     * Find and return an existing ImageCache stored in a {@link RetainFragment}, if not found a new
     * one is created with defaults and saved to a {@link RetainFragment}.
     * 
     * @param activity The calling {@link FragmentActivity}
     * @param uniqueName A unique name to append to the cache directory
     * @return An existing retained ImageCache object or a new one if one did not exist.
     */
    public static ImageCache findOrCreateCache(final FragmentActivity activity, final String uniqueName) {
        return findOrCreateCache(activity, new CacheParams(uniqueName));
    }

    /**
     * Find and return an existing ImageCache stored in a {@link RetainFragment}, if not found a new
     * one is created using the supplied params and saved to a {@link RetainFragment}.
     * 
     * @param activity The calling {@link FragmentActivity}
     * @param cacheParams The cache parameters to use if creating the ImageCache
     * @return An existing retained ImageCache object or a new one if one did not exist
     */
    public static ImageCache findOrCreateCache(final FragmentActivity activity, CacheParams cacheParams) {

        // Search for, or create an instance of the non-UI RetainFragment
        final RetainFragment mRetainFragment = RetainFragment.findOrCreateRetainFragment(activity.getSupportFragmentManager(), cacheParams.uniqueName);

        // See if we already have an ImageCache stored in RetainFragment
        ImageCache imageCache = (ImageCache) mRetainFragment.getObject();

        // No existing ImageCache, create one and store it in RetainFragment
        if (imageCache == null) {
            imageCache = new ImageCache(activity, cacheParams);
            mRetainFragment.setObject(imageCache);
        }

        return imageCache;
    }

    /**
     * Sets the target compression format and quality for images written to the disk cache.
     * 
     * @param compressFormat
     * @param quality
     */
    public void setCompressParams(CompressFormat compressFormat, int quality) {
        mCompressFormat = compressFormat;
        mCompressQuality = quality;
    }
    
    public void addToCache(String key, Bitmap value, CompressFormat format, int quality) {
        if (key == null || value == null) {
            return;
        }

        // Add to memory cache
        if (mMemoryCache != null && mMemoryCache.get(key) == null) {
            mMemoryCache.put(key, value);
        }

        // Add to disk cache
        if (mDiskCache != null && !mDiskCache.containsKey(key)) {
            String file = mDiskCache.createFilePath(key);
            try {
                if (writeToFile(value, file, format, quality)) {
                    mDiskCache.add(key, file);
                }
            } catch (final FileNotFoundException e) {
                Log.e(TAG, "Error in put: " + e.getMessage());
            } catch (final IOException e) {
                Log.e(TAG, "Error in put: " + e.getMessage());
            }

        }
    }
    
    protected boolean writeToFile(Bitmap bitmap, String file, CompressFormat format, int quality) throws IOException, FileNotFoundException {
        
        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file), ImageCacheUtils.IO_BUFFER_SIZE);
            return bitmap.compress(format, quality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    protected boolean writeToFile(Bitmap bitmap, String file) throws IOException, FileNotFoundException {

        OutputStream out = null;
        try {
            out = new BufferedOutputStream(new FileOutputStream(file), ImageCacheUtils.IO_BUFFER_SIZE);
            return bitmap.compress(mCompressFormat, mCompressQuality, out);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    @Override
    protected int getSize(Bitmap data) {
        return ImageCacheUtils.getBitmapSize(data);
    }

    @Override
    protected Bitmap readValueFromDisk(File file) throws IOException {
        try {
            if (file != null) {
                return BitmapFactory.decodeFile(file.getAbsolutePath());
            }
        } catch (Throwable e) {
        }
        return null;
    }
    
    public File getFile(String key) {
        if (mDiskCache != null) {
            return mDiskCache.get(key);
        }
        return null;
    }

}
