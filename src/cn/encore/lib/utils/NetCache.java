package cn.encore.lib.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import android.content.Context;

/**
 * 网络请求缓存类
 * 
 * <p>
 * 作用：用于存储访问网络的数据。
 * </p>
 * <p>
 * 具体实现：将访问网络得到的数据序列化到本地。当再次需要使用时，根据请求地址，获取对应数据的Bean对象。
 * </p>
 * 
 * @author Perry
 * 
 */
public class NetCache {

	/*
	 * 使用例子 try {
	 * 
	 * //将musicInfos序列化到本地 NetCache.saveCache(musicInfos, "http://www.123.com");
	 * 
	 * //根据网络请求，返回其对象。 List<MusicInfo> musicInfos2 =
	 * NetCache.readCache("http://www.123.com");
	 * 
	 * } catch (IOException e) { // 读写错误 e.printStackTrace(); } catch
	 * (ClassNotFoundException e) { // 当找不到对应的Bean对象来装载数据，则会抛出该异常。
	 * e.printStackTrace(); }
	 */

	/** 缓存最长时间 7天 单位：毫秒 */
	private static final long CLEAN_CACHE_DEADLINE = 7 * 24 * 60 * 60 * 1000;

	private static Set<String> CacheNameSets;

	/**
	 * 把网络数据对象序列化。
	 * 
	 * @param bean
	 *            将要序列化的Bean对象
	 * @param url
	 *            请求Bean对象对应的URL地址
	 * @throws IOException
	 * @author Perry
	 */
	public static <T> void saveCache(Context context, T bean, String url) throws IOException {
		if(bean == null){
			return;
		}
		String MD5Name = MD5Util.getMD5String(url);
		String netCacheString = Util.getNetCache(context);
		if (netCacheString == null) {
			return;
		}
		String filePath = netCacheString + "/" + MD5Name;

		// 判断存放缓存的文件夹是否存在。若不存在，则生成该文件夹。
		File cacheFolder = new File(netCacheString);
		if (!cacheFolder.exists()) {
			cacheFolder.mkdirs();
		}

		if (CacheNameSets == null) {
			CacheNameSets = new HashSet<String>();
			String[] cacheFileName = cacheFolder.list();
			if (cacheFileName != null) {
				for (String name : cacheFileName) {
					CacheNameSets.add(name);
				}
			}
		}

		/* 写本地序列化对象 */
		FileOutputStream fileOutputStream = new FileOutputStream(filePath);
		// GZIP压缩数据。并起到加密的作用。
		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);

		ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);
		objectOutputStream.writeObject(bean);
		objectOutputStream.flush();
		objectOutputStream.close();
		if (!CacheNameSets.contains(MD5Name)) {
			CacheNameSets.add(MD5Name);
		}
	}

	/**
	 * 读取本地缓存数据。
	 * 
	 * @param url
	 *            在线请求的地址。
	 * @return 返回与请求地址对应数据的Bean对象。
	 * @throws IOException
	 *             如果没有请求地址对应的缓存数据，则会抛出该异常。
	 * @throws ClassNotFoundException
	 *             当找不到对应的Bean对象来装载数据，则会抛出该异常。
	 * @throws ClassCastException
	 *             当前已经序列化到本地的对象，与读取出来装在对象不一致，则会抛出异常。
	 * @author Perry
	 */
	public static <T> T readCache(Context context, String url) {
		String MD5Name = MD5Util.getMD5String(url);
		String netCacheString = Util.getNetCache(context);
		if (netCacheString == null) {
			return null;
		}
		String filePath = netCacheString + "/" + MD5Name;

		File cacheFolderFile = new File(netCacheString);
		if (!cacheFolderFile.exists()) {// 当前存储cache的文件夹不存在，直接抛出异常。
			return null;
		}
		if (CacheNameSets == null) {
			CacheNameSets = new HashSet<String>();
			String[] cacheFileName = cacheFolderFile.list();
			if (cacheFileName != null) {
				for (String name : cacheFileName) {
					CacheNameSets.add(name);
				}
			}
		} else {
			if (!CacheNameSets.contains(MD5Name)) {
				// TODO 建议：缓存不存在，是经常发生的事情，属正常业务逻辑，建议缓存不存在的通知方式改为返回null，而不是抛异常
				return null;
			}
		}

		/* 读取本地序列化对象 */
		FileInputStream fileInputStream;
		try {
			fileInputStream = new FileInputStream(filePath);
			// GZIP解压数据。
			GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
			ObjectInputStream objectInputStream = new ObjectInputStream(gzipInputStream);
			@SuppressWarnings("unchecked")
			T bean = (T) objectInputStream.readObject();

			gzipInputStream.close();
			objectInputStream.close();
			gzipInputStream = null;
			objectInputStream = null;

			return bean;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

//	/**
//	 * 检查并删除超过限定时间的缓存数据。
//	 * 
//	 * @author Perry
//	 */
//	public static void checkAndCleanCache() {
//
//		long currentTimeMillis = System.currentTimeMillis();
//
//		File cacheFolder = new File(Constant.SdcardPath.CACHE_SAVEPATH);
//		// 判断存放缓存的文件夹是否存在。若不存在，则生成该文件夹。
//		if (!cacheFolder.exists()) {
//			cacheFolder.mkdirs();
//			return;
//		}
//		File[] cacheFiles = cacheFolder.listFiles();
//		if (cacheFiles == null) {// 当前文件夹中无任何缓存文件。
//			return;
//		}
//		List<File> cleanFileList = new ArrayList<File>();
//		// 遍历文件 查找出超出限定日期的缓存文件。
//		for (File file : cacheFiles) {
//			if (file.lastModified() < currentTimeMillis - CLEAN_CACHE_DEADLINE)
//				cleanFileList.add(file);
//		}
//
//		// 删除 超出限定日期的缓存文件
//		for (File file : cleanFileList) {
//			if (file.exists())
//				file.delete();
//		}
//	}
}
