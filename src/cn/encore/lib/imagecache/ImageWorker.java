/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.encore.lib.imagecache;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.os.AsyncTask;
import android.widget.ImageView;
import cn.encore.lib.utils.Log;


/**
 * This class wraps up completing some arbitrary long running work when loading a bitmap to an
 * ImageView. It handles things like using a memory and disk cache, running the work in a background
 * thread and setting a placeholder image.
 */
public abstract class ImageWorker {
    private static final String TAG = "ImageWorker";
    private static final int FADE_IN_TIME = 400;
    
    private ImageCache mImageCache;
    private Bitmap mLoadingBitmap;
    private boolean mFadeInBitmap = true;
    private boolean mExitTasksEarly = false;

    protected Context mContext;
    protected ImageWorkerAdapter mImageWorkerAdapter;
    
    protected CompressFormat mCompressFormat = null;
    protected int mCompressQuality = 0;

    protected ImageWorker(Context context) {
        mContext = context;
    }
    
    public ImageWorker setCompressParams(CompressFormat compressFormat, int quality) {
        mCompressFormat = compressFormat;
        mCompressQuality = quality;
        return this;
    }

    /**
     * {@link #loadImage(Object, ImageView, ImageLoadingListener)}
     */
    public void loadImage(Object data, ImageView imageView) {
        loadImage(data, imageView, null);
    }
    
    /**
     * Load an image specified by the data parameter into an ImageView (override
     * {@link ImageWorker#processBitmap(Object)} to define the processing logic). A memory and disk
     * cache will be used if an {@link ImageCache} has been set using
     * {@link ImageWorker#setImageCache(ImageCache)}. If the image is found in the memory cache, it
     * is set immediately, otherwise an {@link AsyncTask} will be created to asynchronously load the
     * bitmap.
     * 
     * @param data The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     * @param listener The loading listener.
     */
    public void loadImage(Object data, ImageView imageView, ImageLoadingListener listener) {
        Bitmap bitmap = null;

        if (mImageCache != null) {
            final String key = getKey(String.valueOf(data));
            bitmap = mImageCache.getFromMemCache(key);
        }

        if (bitmap != null) {
            // Bitmap found in memory cache
            imageView.setImageBitmap(bitmap);
            if(listener != null) {
                listener.onLoadingComplete(bitmap);
            }
        } else if (cancelPotentialWork(data, imageView)) {
            final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
            IAsyncDrawable asyncDrawable = null;
            if (mLoadingBitmap == null || mLoadingBitmap.getNinePatchChunk() == null) {
                asyncDrawable = new BitmapAsyncDrawable(mContext.getResources(), mLoadingBitmap, task, listener);
            } else {
                asyncDrawable = new NinePatchAsyncDrawable(mContext.getResources(), mLoadingBitmap, task, listener);
            }
            imageView.setImageDrawable((Drawable)asyncDrawable);
            task.execute(data);
        }
    }

    /**
     * Load an image specified from a set adapter into an ImageView (override
     * {@link ImageWorker#processBitmap(Object)} to define the processing logic). A memory and disk
     * cache will be used if an {@link ImageCache} has been set using
     * {@link ImageWorker#setImageCache(ImageCache)}. If the image is found in the memory cache, it
     * is set immediately, otherwise an {@link AsyncTask} will be created to asynchronously load the
     * bitmap. {@link ImageWorker#setAdapter(ImageWorkerAdapter)} must be called before using this
     * method.
     * 
     * @param data The URL of the image to download.
     * @param imageView The ImageView to bind the downloaded image to.
     */
    public void loadImage(int num, ImageView imageView) {
        if (mImageWorkerAdapter != null) {
            loadImage(mImageWorkerAdapter.getItem(num), imageView);
        } else {
            throw new NullPointerException("Data not set, must call setAdapter() first.");
        }
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is running.
     * 
     * @param bitmap
     */
    public ImageWorker setLoadingImage(Bitmap bitmap) {
        mLoadingBitmap = bitmap;
        return this;
    }

    /**
     * Set placeholder bitmap that shows when the the background thread is running.
     * 
     * @param resId
     */
    public ImageWorker setLoadingImage(int resId) {
        try {
            mLoadingBitmap = BitmapFactory.decodeResource(mContext.getResources(), resId);
        } catch (Throwable e) {
        }
        return this;
    }

    /**
     * Set the {@link ImageCache} object to use with this ImageWorker.
     * 
     * @param imageCache
     */
    public ImageWorker setImageCache(ImageCache imageCache) {
        mImageCache = imageCache;
        return this;
    }

    public ImageCache getImageCache() {
        return mImageCache;
    }

    /**
     * If set to true, the image will fade-in once it has been loaded by the background thread.
     * 
     * @param fadeIn
     */
    public ImageWorker setImageFadeIn(boolean fadeIn) {
        mFadeInBitmap = fadeIn;
        return this;
    }

    /**
     * 是否更早退出设置图片任务
     * 
     * @param exitTasksEarly
     */
    public void setExitTasksEarly(boolean exitTasksEarly) {
        mExitTasksEarly = exitTasksEarly;
    }

    /**
     * Subclasses should override this to define any processing or work that must happen to produce
     * the final bitmap. This will be executed in a background thread and be long running. For
     * example, you could resize a large bitmap here, or pull down an image from the network.
     * 
     * @param data The data to identify which image to process, as provided by
     *            {@link ImageWorker#loadImage(Object, ImageView)}
     * @param imageViewReference 
     * @return The processed bitmap
     */
    protected abstract Bitmap processBitmap(Object data, WeakReference<ImageView> imageViewReference);

    public static void cancelWork(ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if (bitmapWorkerTask != null) {
            bitmapWorkerTask.cancel(true);
                final Object bitmapData = bitmapWorkerTask.data;
                Log.d(TAG, "cancelWork - cancelled work for " + bitmapData);
        }
    }

    /**
     * Returns true if the current work has been canceled or if there was no work in progress on
     * this image view. Returns false if the work in progress deals with the same data. The work is
     * not stopped in that case.
     */
    public static boolean cancelPotentialWork(Object data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Object bitmapData = bitmapWorkerTask.data;
            if (bitmapData == null || !bitmapData.equals(data)) {
                bitmapWorkerTask.cancel(true);
                    Log.d(TAG, "cancelPotentialWork - cancelled work for " + data);
            } else {
                // The same work is already in progress.
                return false;
            }
        }
        return true;
    }

    /**
     * @param imageView Any imageView
     * @return Retrieve the currently active work task (if any) associated with this imageView. null
     *         if there is no such task.
     */
    static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof IAsyncDrawable) {
                final IAsyncDrawable asyncDrawable = (IAsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }
    
    static ImageLoadingListener getImageLoadLinsener(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof IAsyncDrawable) {
                final IAsyncDrawable asyncDrawable = (IAsyncDrawable) drawable;
                return asyncDrawable.getImageLoadingListener();
            }
        }
        return null;
    }

    /**
     * The actual AsyncTask that will asynchronously process the image.
     */
    class BitmapWorkerTask extends OptimizedAsyncTask<Object, Object, Bitmap> {
        private Object data;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(ImageView imageView) {
            imageViewReference = new WeakReference<ImageView>(imageView);
        }
        
        @Override
        protected void onPreExecute() {
            ImageView imageView = imageViewReference.get();
            ImageLoadingListener listener = getImageLoadLinsener(imageView);
            if (listener != null) {
                listener.onLoadingStarted();
            }
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            int type = (Integer) values[0];
            switch (type) {
            case PublishType.ON_UPDATE: {
                float percent = (Float) values[1];
                final ImageView imageView = getAttachedImageView();
                ImageLoadingListener listener = getImageLoadLinsener(imageView);
                    Log.d(TAG, "downloadBitmap - downloading : " + percent);
                if (listener != null) {
                    listener.onUpdateDownload(percent);
                }
                break;
            }
            case PublishType.ON_END: {
                Bitmap bitmap = (Bitmap) values[1];
                final ImageView imageView = getAttachedImageView();
                ImageLoadingListener listener = getImageLoadLinsener(imageView);
                if (listener != null) {
                    if (bitmap != null) {
                        listener.onLoadingComplete(bitmap);
                    } else {
                        listener.onLoadingFailed(FailReason.UNKNOWN);
                    }
                }
                break;
            }
            }
        }

        /**
         * Background processing.
         */
        @Override
        protected Bitmap doInBackground(Object... params) {
            data = params[0];
            final String dataString = String.valueOf(data);
            final String key = getKey(dataString);
            Bitmap bitmap = null;

            // If the image cache is available and this task has not been cancelled by another
            // thread and the ImageView that was originally bound to this task is still bound back
            // to this task and our "exit early" flag is not set then try and fetch the bitmap from
            // the cache
            if (mImageCache != null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                bitmap = mImageCache.getFromDiskCache(key);
            }
            
            //load in wifi only
            if (mWifiOnly && !isWifi()) {
                mImageCache.addToCache(key, bitmap);
                return bitmap;
            }

            // If the bitmap was not found in the cache and this task has not been cancelled by
            // another thread and the ImageView that was originally bound to this task is still
            // bound back to this task and our "exit early" flag is not set, then call the main
            // process method (as implemented by a subclass)
            if (bitmap == null && !isCancelled() && getAttachedImageView() != null && !mExitTasksEarly) {
                bitmap = processBitmap(dataString, imageViewReference);
            }
            
            publishProgress(PublishType.ON_END, bitmap);

            // If the bitmap was processed and the image cache is available, then add the processed
            // bitmap to the cache for future use. Note we don't check if the task was cancelled
            // here, if it was, and the thread is still running, we may as well add the processed
            // bitmap to our cache as it might be used again in the future
            if (bitmap != null && mImageCache != null) {
                if (mCompressFormat == null || mCompressQuality == 0) {
                    mImageCache.addToCache(key, bitmap);
                } else {
                    mImageCache.addToCache(key, bitmap, mCompressFormat, mCompressQuality);
                }
            }

            return bitmap;
        }

        /**
         * Once the image is processed, associates it to the imageView
         */
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            // if cancel was called on this task or the "exit early" flag is set then we're done
            if (isCancelled() || mExitTasksEarly) {
                bitmap = null;
            }

            final ImageView imageView = getAttachedImageView();
            if (bitmap != null && imageView != null) {
                setImageBitmap(imageView, bitmap);
            }
        }

        /**
         * Returns the ImageView associated with this task as long as the ImageView's task still
         * points to this task as well. Returns null otherwise.
         */
        private ImageView getAttachedImageView() {
            final ImageView imageView = imageViewReference.get();
            final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

            if (this == bitmapWorkerTask) {
                return imageView;
            }

            return null;
        }
    }

    /**
     * A custom Drawable that will be attached to the imageView while the work is in progress.
     * Contains a reference to the actual worker task, so that it can be stopped if a new binding is
     * required, and makes sure that only the last started worker process can bind its result,
     * independently of the finish order.
     */
    private static class BitmapAsyncDrawable extends BitmapDrawable implements IAsyncDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;
        private final ImageLoadingListener listener;

        public BitmapAsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask, ImageLoadingListener listener) {
            super(res, bitmap);

            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
            this.listener = listener; 
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
        
        public ImageLoadingListener getImageLoadingListener() {
            return listener;
        }
    }
    
    private static class NinePatchAsyncDrawable extends NinePatchDrawable implements IAsyncDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;
        private final ImageLoadingListener listener;
        
        public NinePatchAsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask, ImageLoadingListener listener) {
            super(res, bitmap, bitmap.getNinePatchChunk(), new Rect(), null);
            
            bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
            this.listener = listener; 
        }
        
        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
        
        public ImageLoadingListener getImageLoadingListener() {
            return listener;
        }
    }
    
    private static interface IAsyncDrawable {
        public BitmapWorkerTask getBitmapWorkerTask();
        
        public ImageLoadingListener getImageLoadingListener();
    }

    /**
     * Called when the processing is complete and the final bitmap should be set on the ImageView.
     * 
     * @param imageView
     * @param bitmap
     */
    private void setImageBitmap(ImageView imageView, Bitmap bitmap) {
        if (mFadeInBitmap) {
            // Transition drawable with a transparent drwabale and the final bitmap
            final TransitionDrawable td = new TransitionDrawable(new Drawable[] { new ColorDrawable(android.R.color.transparent), new BitmapDrawable(mContext.getResources(), bitmap) });
            // Set background to loading bitmap
            // imageView.setBackgroundDrawable(new BitmapDrawable(mContext.getResources(), mLoadingBitmap));

            imageView.setImageDrawable(td);
            td.startTransition(FADE_IN_TIME);
        } else {
            imageView.setImageBitmap(bitmap);
        }
    }

    public abstract String getKey(String dataString);

    /**
     * Set the simple adapter which holds the backing data.
     * 
     * @param adapter
     */
    public ImageWorker setAdapter(ImageWorkerAdapter adapter) {
        mImageWorkerAdapter = adapter;
        return this;
    }

    /**
     * Get the current adapter.
     * 
     * @return
     */
    public ImageWorkerAdapter getAdapter() {
        return mImageWorkerAdapter;
    }

    /**
     * A very simple adapter for use with ImageWorker class and subclasses.
     */
    public static abstract class ImageWorkerAdapter {
        public abstract Object getItem(int num);

        public abstract int getSize();
    }
    
    static interface PublishType {
        int ON_END = 0;
        int ON_UPDATE = 1;
    }
    
    private static boolean mWifiOnly;
    private boolean isWifi() {
        ConnectivityManager manager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        State wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if (wifi == State.CONNECTED || wifi == State.CONNECTING)
            return true;
        return false;
    }

    public static boolean isWifiOnly() {
        return mWifiOnly;
    }

    public static void setWifiOnly(boolean wifiOnly) {
        mWifiOnly = wifiOnly;
    }
}
