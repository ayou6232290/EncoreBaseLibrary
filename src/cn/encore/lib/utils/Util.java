package cn.encore.lib.utils;

import android.content.res.Resources;
import android.util.TypedValue;

public class Util {

	public static int dpToPx(Resources res, int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
	}

	/**
	 * 拼接从图片资源文件拼接URI
	 * @param resId
	 * @return
	 */
	public static String getImageCacheByResIdUri(int resId){
		return "drawable://" + resId;
	}
	
	/**
	 * 拼接从asset文件拼接URI
	 * @param resId
	 * @return
	 */
	public static String getImageCacheByAssetIdUri(String assetFileName){
		return "assets://" + assetFileName;
	}
	
	/**
	 * 拼接从内存卡拼接URI
	 * @param resId
	 * @return
	 */
	public static String getImageCacheBySdcardIdUri(String fileName){
		return "file://" + fileName;
	}
}
