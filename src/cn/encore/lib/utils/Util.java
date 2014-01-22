package cn.encore.lib.utils;

import java.util.Random;

import android.content.res.Resources;
import android.util.TypedValue;

public class Util {

	public static int dpToPx(Resources res, int dp) {
		return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
	}

	/**
	 * 拼接从图片资源文件拼接URI
	 * 
	 * @param resId
	 * @return
	 */
	public static String getImageCacheByResIdUri(int resId) {
		return "drawable://" + resId;
	}

	/**
	 * 拼接从asset文件拼接URI
	 * 
	 * @param resId
	 * @return
	 */
	public static String getImageCacheByAssetIdUri(String assetFileName) {
		return "assets://" + assetFileName;
	}

	/**
	 * 拼接从内存卡拼接URI
	 * 
	 * @param resId
	 * @return
	 */
	public static String getImageCacheBySdcardIdUri(String fileName) {
		return "file://" + fileName;
	}
	
	/**
	 * 随机某个范围内的数字
	 * @param max 最大随机数字
	 * @param min 最小随机数字
	 * @return
	 */
	public static int randomIntNumber(int max, int min) {
		Random random = new Random();
		int s = random.nextInt(max) % (max - min + 1) + min;
		return s;
	}
}
