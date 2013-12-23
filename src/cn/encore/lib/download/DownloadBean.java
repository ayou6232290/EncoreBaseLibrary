package cn.encore.lib.download;

import java.io.Serializable;

/**
 * [一句话功能简述]<br/>
 * 
 * @author Encore
 * @date 2012-8-23
 */
public class DownloadBean implements Serializable{
	private static final long serialVersionUID = 4067553596092978147L;
	public String downloadUrl;
	public String savePath;
	public Object tag;
	public boolean loadFinish;

	/**
	 * 下载偏移，用于断点续传
	 */
	public long    downloadOffset;
	public long    downloadSize;
	
	/** 下载时是否检查网络，默认检查，互传不检查，因为互传可能创建了AP */
	public boolean checkNetwork = true;
}