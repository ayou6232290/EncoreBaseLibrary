/*
 * 文件名: Log.java
 * 版    权：深圳市快播科技有限公司
 * 描    述: 
 * 创建人: 胡启明
 * 创建时间:2012-7-16
 * 
 * 修改人：
 * 修改时间:
 * 修改内容：[修改内容]
 */
package cn.encore.lib.utils;


/**
 * @author Encore.liang
 * @date 2012-7-16
 */
public class Log {
	
	public static boolean DEBUG = false;
	
	public static void setDebug(boolean isDebug) {
		DEBUG = isDebug;
	}
	
	public static void d(String tag, String msg) {
		if (DEBUG) 
		android.util.Log.d(tag, msg);
	}

	public static void i(String tag, String msg) {
		if (DEBUG)
		android.util.Log.i(tag, msg);
	}

	public static void w(String tag, String msg) {
		if (DEBUG)
		android.util.Log.w(tag, msg);
	}

	public static void e(String tag, String msg) {
		if (DEBUG)
		android.util.Log.e(tag, msg);
	}
	
	public static void v(String tag, String msg) {
		if (DEBUG)
		android.util.Log.v(tag, msg);
	}
}
