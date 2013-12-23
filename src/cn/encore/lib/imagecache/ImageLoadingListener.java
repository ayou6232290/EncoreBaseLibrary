package cn.encore.lib.imagecache;

import android.graphics.Bitmap;
import android.widget.ImageView;

public interface ImageLoadingListener {
    /**
     * 第一次取图片时,更新下载进度
     * @param percent
     */
    void onUpdateDownload(float percent);

    /**
     * 第一次取到图片时,在这里处理图片效果
     * @param bitmap 要处理的图片
     * @return 返回处理过的图片
     */
    Bitmap onAfterDownload(Bitmap bitmap);

    /** Is called when image loading task was started */
    void onLoadingStarted();

    /** Is called when an error was occurred during image loading */
    void onLoadingFailed(FailReason failReason);

    /** Is called when image is loaded successfully and displayed in {@link ImageView} */
    void onLoadingComplete(Bitmap loadedImage);

    /** Is called when image loading task was cancelled because {@link ImageView} was reused in newer task */
    void onLoadingCancelled();
}
