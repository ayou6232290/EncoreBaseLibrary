package cn.encore.lib.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import android.os.Environment;
import android.os.StatFs;

/**
 * [SDcard工具类]
 */
public class SdcardUtil {

	private final static String TAG= "SdcardUtil";
	
	public static StringBuilder fatFileSystem = new StringBuilder();
	
	public static boolean checkSDPath() {
		return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
	}

	public static String getSDPath() {
		if (checkSDPath()) {
			return Environment.getExternalStorageDirectory().toString();
		}
		return null;
	}
	
	/**
	 * 选择最佳路径
	 * 
	 * @param findPath
	 * @return
	 */
	private static String[] filterBetterExternalPath(String[] findPath) {

		if (findPath != null && findPath.length > 0) {

			//Log.i(TAG, "filterBetterExternalPath-> findPath=" + Arrays.toString(findPath).toString()
			//		+ ",length=" + findPath.length);

			int len = 0;
			for (int i = 0; i < findPath.length; i++) {

				for (int j = i; j < findPath.length - i; j++) {
					if (findPath[i] == null || findPath[j] == null) {
						continue;
					}
					File fileA = new File(findPath[i]);
					File fileB = new File(findPath[j]);

					if (fileA.getParent().equals(findPath[j])) {
						findPath[i] = null;
						continue;
					}

					if (fileB.getParent().equals(findPath[i])) {
						findPath[j] = null;
						continue;
					}

				}

			}

			for (int i = 0; i < findPath.length; i++) {
				if (findPath[i] != null) {
					len++;
				}
			}

			String[] newPath = new String[len];
			int n = 0;
			for (int i = 0; i < findPath.length; i++) {
				if (findPath[i] != null) {
					newPath[n++] = findPath[i];
				}
			}

			//Log.i(TAG, "BetterExternalPath->" + Arrays.toString(newPath));
			return newPath;
		}

		return null;
	}

	public static String findStoragePath() {
		String[] findPath = add2Array(filterEmptyPadh(findExternalPath()), getSDPath());
		// find the largest free size sd card
		if (findPath == null || findPath.length == 0) {
			return null;
		}
		long freeSize = -1;
		String paString = null;
		for (String string : findPath) {
			if (checkSDPath() && string != null) {
				long size = getAvailaleSize(string);
				//Log.i(TAG, string + " free size : " + size);
				if (freeSize < size) {
					paString = string;
				}
				freeSize = freeSize < size ? size : freeSize;
			}
		}
		return paString;
	}

	public static String[] getAvailableSdCardPath() {
		String[] findPath = add2Array(filterEmptyPadh(findExternalPath()), getSDPath());
		return findPath;
	}
	
	/**
	 * 过滤空目录
	 * 
	 * @param path
	 * @return
	 */
	private static String[] filterEmptyPadh(String[] path) {

		if (path != null && path.length > 0) {

			int n = 0;
			for (int i = 0; i < path.length; i++) {

				File file = new File(path[i]);
				if (file.list() != null) {
					n++;
				} else {
					path[i] = null;
				}

			}

			String[] newPath = new String[n];
			int i = 0;
			for (String fp : path) {
				if (fp != null) {
					newPath[i++] = fp;
				}
			}

			return newPath;
		}
		return null;
	}
	
	/**
	 * 合并数组-不重复
	 * 
	 * @param arr
	 * @param str
	 * @return
	 */
	private static String[] add2Array(String[] arr, String str) {

		String[] newArray = null;
		if (arr != null && arr.length > 0) {
			boolean flag = false;
			for (String value : arr) {
				if (value.equals(str)) {
					flag = true;
					break;
				}
			}

			if (flag) {
				newArray = arr;
			} else {
				newArray = new String[arr.length + 1];
				newArray[0] = str;
				for (int i = 1; i < arr.length; i++) {
					newArray[i] = arr[i];
				}

			}

		}

		return newArray;
	}
	
	public static boolean isFat() {
		boolean isfat = false;
		findExternalPath();
		if (isNotBlank(fatFileSystem.toString())) {
			String[] fats = filterEmptyPadh(fatFileSystem.toString().split(","));

			String sdcard = getSDPath();
			if (isNotBlank(sdcard)) {

				for (String fat : fats) {
					if (sdcard.equals(fat)) { 
						isfat = true;
						break;
					}
				}

			}

		}

		return isfat;
	}

	public static boolean isFat(String pathSdcard) {
		boolean isfat = false;
		findExternalPath();
		if (isNotBlank(fatFileSystem.toString())) {
			String[] fats = filterEmptyPadh(fatFileSystem.toString().split(","));

			String sdcard = pathSdcard;
			if (isNotBlank(sdcard)) {

				for (String fat : fats) {
					if (sdcard.equals(fat)) {
						isfat = true;
						break;
					}
				}

			}

		}

		return isfat;
	}
	
	public static boolean isNotBlank(String[] arr) {
		if (arr != null && arr.length > 0) {
			return true;
		}
		return false;
	}

	public static boolean isBlank(String[] arr) {
		return !isNotBlank(arr);
	}

	public static boolean isNotBlank(List<?> list) {
		if (list != null && list.size() > 0) {
			return true;
		}
		return false;
	}

	public static boolean isBlank(List<?> list) {
		return !isNotBlank(list);
	}

	public static boolean isNotBlank(String str) {
		if (str != null && !"".equals(str)) {
			return true;
		}
		return false;
	}

	public static boolean isBlank(String str) {
		return !isNotBlank(str);
	}
	
	//Start----SystemUtility.java
	
	public static long getAvailaleSize(String pathStr) {
		File path = new File(pathStr); // 取得sdcard文件路径
		if(! path.exists()) {
			return -1;
		}
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize;
	}
	
	/**
	 *
	 *  获取sd卡的大小
	 * @param pathStr sd卡的路径
	 * @return sd卡的大小，单位byte
	 */
	public static long getSdCardSize(String pathStr) {
		File path = new File(pathStr); 
		if(! path.exists()) {
			return -1;
		}
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		return totalBlocks * blockSize;
	}
	
	public static String[] getExternalPath() {
		String[] findPath = add2Array(filterEmptyPadh(findExternalPath()), getSDPath());
		return filterBetterExternalPath(findPath);
	}
	
	public static String[] findExternalPath(){
		InputStream is = null;
		InputStreamReader ir = null;
		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();
		try {
			is = new FileInputStream("/proc/mounts");
			ir = new InputStreamReader(is);
			br = new BufferedReader(ir);
			StringBuilder mountStringBuilder = new StringBuilder();
			
			String line = "";
			while ((line = br.readLine())!=null) {
				mountStringBuilder.append(line);
				mountStringBuilder.append("\n");
				if (line.contains("fuse rw")) {
					
					int ep = line.indexOf("fuse rw");
					line = line.substring(0, ep-1);
					int sp = line.lastIndexOf(" ");
					line = line.substring(sp+1);
					sb.append(line).append(",");
					
				}else if (line.contains("vfat rw")) {
					
					int ep = line.indexOf("vfat rw");
					line = line.substring(0, ep-1);
					int sp = line.lastIndexOf(" ");
					line = line.substring(sp+1);
					sb.append(line).append(",");
					fatFileSystem.append(line).append(",");
				}
				

			}
//			Log.d(TAG, mountStringBuilder.toString());
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			try {
				br.close();
				ir.close();
				is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (sb.length() > 0) {
			String[] result = sb.toString().split(",");
			if (result != null && result.length > 0) {
				
				//Log.i(TAG, "findExternalPath-> result="+Arrays.toString(result).toString()+",length="+result.length);
				return result;
			}
		}
		
		return null;
	}
}

