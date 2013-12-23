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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.widget.ImageView;
import cn.encore.lib.utils.Log;


/**
 * A simple subclass of {@link ImageResizer} that fetches and resizes images fetched from a URL.
 */
public class ImageFetcher extends ImageResizer {
    private static final String TAG = "ImageFetcher";
    private static final int HTTP_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String HTTP_CACHE_DIR = "http";

    /**
     * Initialize providing a target image width and height for the processing images.
     * 
     * @param context
     * @param imageWidth
     * @param imageHeight
     */
    public ImageFetcher(Context context, int imageWidth, int imageHeight) {
        super(context, imageWidth, imageHeight);
        init(context);
    }

    /**
     * Initialize providing a single target image size (used for both width and height);
     * 
     * @param context
     * @param imageSize
     */
    public ImageFetcher(Context context, int imageSize) {
        super(context, imageSize);
        init(context);
    }

    private void init(Context context) {
        checkConnection(context);
    }

    /**
     * Simple network connection check.
     * 
     * @param context
     */
    private void checkConnection(Context context) {
        final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if (networkInfo == null || !networkInfo.isConnectedOrConnecting()) {
//            Toast.makeText(context, "No network connection found.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "checkConnection - no connection found");
        }
    }

    /**
     * The main process method, which will be called by the ImageWorker in the AsyncTask background
     * thread.
     * 
     * @param data The data to load the bitmap, in this case, a regular http URL
     * @return The downloaded and resized bitmap
     */
    private Bitmap processBitmap(String data, WeakReference<ImageView> imageViewReference) {
            Log.d(TAG, "processBitmap - " + data);

        /*
         * // Download a bitmap, write it to a file final File f = downloadBitmap(mContext, data);
         * 
         * if (f != null) { // Return a sampled down version return
         * decodeSampledBitmapFromFile(f.toString(), mImageWidth, mImageHeight); }
         */
        try {
            return createFromUri(mContext, data, mImageWidth, mImageHeight, imageViewReference);
        } catch (OutOfMemoryError e) {
                e.printStackTrace();
        } catch (IOException e) {
                e.printStackTrace();
        } catch (URISyntaxException e) {
                e.printStackTrace();
        }

        return null;
    }

    @Override
    protected Bitmap processBitmap(Object data, WeakReference<ImageView> imageViewReference) {
        return processBitmap(String.valueOf(data), imageViewReference);
    }

    /**
     * Download a bitmap from a URL, write it to a disk and return the File pointer. This
     * implementation uses a simple disk cache.
     * 
     * @param context The context to use
     * @param urlString The URL to fetch
     * @param imageViewReference 
     * @return A File pointing to the fetched bitmap
     */
    public static File downloadBitmap(Context context, String urlString, WeakReference<ImageView> imageViewReference) {
        final File cacheDir = DiskLruCache.getDiskCacheDir(context, HTTP_CACHE_DIR);

        final DiskLruCache cache = DiskLruCache.openCache(cacheDir, HTTP_CACHE_SIZE);
        if (cache == null) {
            // FIXME 提示用户磁盘空间不足
            Intent intent = new Intent(AbstractCache.INTENT_NOT_ENOUGH_SPACE);
            context.sendBroadcast(intent);
            return null;
        }

        final String path = cache.createFilePath(urlString);
        final File tempFile = new File(path + "." + System.currentTimeMillis());
        final File cacheFile = new File(path);

        if (cache.containsKey(urlString)) {
                Log.d(TAG, "downloadBitmap - found in http cache - " + urlString);
            return cacheFile;
        }

            Log.d(TAG, "downloadBitmap - downloading - " + urlString);

        ImageCacheUtils.disableConnectionReuseIfNecessary();
        HttpURLConnection urlConnection = null;
        BufferedOutputStream out = null;

        try {
        	urlString =  URLEncoder.encode(urlString,"utf-8").replaceAll("\\+", "%20");
        	urlString = urlString.replaceAll("%3A", ":").replaceAll("%2F", "/"); 
            final URL url = new URL(urlString);
            urlConnection = (HttpURLConnection) url.openConnection();
            final InputStream in = new BufferedInputStream(urlConnection.getInputStream(), ImageCacheUtils.IO_BUFFER_SIZE);
            out = new BufferedOutputStream(new FileOutputStream(tempFile), ImageCacheUtils.IO_BUFFER_SIZE);
            
            int length = urlConnection.getContentLength();
            int size = 0;

            int b;
            byte[] buffer = new byte[ImageCacheUtils.IO_BUFFER_SIZE];
            while ((b = in.read(buffer)) != -1) {
                out.write(buffer, 0, b);
                ImageView imageView = imageViewReference.get();
                BitmapWorkerTask task = getBitmapWorkerTask(imageView);
                size += b;
                if (task != null) {
                    float percent = (size * 100f) / length;
                    task.publishProgress(PublishType.ON_UPDATE, percent);
                }
            }
            if (!tempFile.renameTo(cacheFile)) {
                tempFile.delete();
            }
            return cacheFile;
        } catch (final IOException e) {
//            Log.e(TAG, "Error in downloadBitmap - " + e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) {
//                    Log.e(TAG, "Error in downloadBitmap - " + e);
                }
            }
        }

        return null;
    }

    /**
     * @param context
     * @param uri 兼容: content:// file:/// http://
     * @param reqWidth
     * @param reqHeight
     * @param imageViewReference 
     * @return
     * @throws IOException
     * @throws URISyntaxException
     * @throws OutOfMemoryError
     */
    public static final Bitmap createFromUri(Context context, String uri, int reqWidth, int reqHeight, WeakReference<ImageView> imageViewReference) throws IOException, URISyntaxException, OutOfMemoryError {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bitmap = null;

        // Get the input stream for computing the sample size.
        BufferedInputStream bufferedInput = null;
        if (uri.startsWith(ContentResolver.SCHEME_CONTENT) || uri.startsWith(ContentResolver.SCHEME_FILE)) {
            // Get the stream from a local file.
            bufferedInput = new BufferedInputStream(context.getContentResolver().openInputStream(Uri.parse(uri)), 16384);
        } else if (uri.startsWith("http://") || uri.startsWith("https://")) {
            // Get the stream from a remote URL.
            File file = downloadBitmap(context, uri, imageViewReference);
            if (file != null) {
                bufferedInput = new BufferedInputStream(new FileInputStream(file));
            }
        }

        // Compute the sample size, i.e., not decoding real pixels.
        if (bufferedInput != null) {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(bufferedInput, null, options);
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        } else {
            return null;
        }

        // Get the input stream again for decoding it to a bitmap.
        bufferedInput.close();
        bufferedInput = null;
        if (uri.startsWith(ContentResolver.SCHEME_CONTENT) || uri.startsWith(ContentResolver.SCHEME_FILE)) {
            // Get the stream from a local file.
            bufferedInput = new BufferedInputStream(context.getContentResolver().openInputStream(Uri.parse(uri)), 16384);
        } else if (uri.startsWith("http://") || uri.startsWith("https://")) {
            // Get the stream from a remote URL.
            File file = downloadBitmap(context, uri, imageViewReference);
            if (file != null) {
                bufferedInput = new BufferedInputStream(new FileInputStream(file));
            }
        }

        // Decode bufferedInput to a bitmap.
        if (bufferedInput != null) {
            options.inJustDecodeBounds = false;
            Thread timeoutThread = new Thread("BitmapTimeoutThread") {
                public void run() {
                    try {
                        Thread.sleep(6000);// 如果6秒后还没有完成解码,就退出解码
                        options.requestCancelDecode();
                    } catch (InterruptedException e) {
                    }
                }
            };
            timeoutThread.start();

            bitmap = BitmapFactory.decodeStream(bufferedInput, null, options);
            bufferedInput.close();
            bufferedInput = null;
        }

        ImageView imageView = imageViewReference.get();
        if (bitmap != null && imageView != null) {//对首次取到的图片进行处理
            ImageLoadingListener listener = getImageLoadLinsener(imageView);
            if (listener != null) {
                bitmap = listener.onAfterDownload(bitmap);
            }
        } else {
            return null;
        }
        
        return bitmap;
    }
}
