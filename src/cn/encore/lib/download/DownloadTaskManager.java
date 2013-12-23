package cn.encore.lib.download;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.os.Environment;
import cn.encore.lib.http.WebUtils;
import cn.encore.lib.utils.Log;
import cn.encore.lib.utils.NetWorkUtils;
import cn.encore.lib.utils.SdcardUtil;

/**
 * [文件下载工具]
 * @date 2012-7-17
 */
public class DownloadTaskManager {

	/**
	 * 错误码,容量不足
	 */
	public static final int ERROR_NO_SPACES = 1;
	
	/**
	 * 错误码，连接错误
	 */
	public static final int ERROR_CANNOT_CONNECT = 2;
	
	/**
	 * 错误码，数据类型错误
	 */
	public static final int ERROR_RESULT = 3;
	
	/**
	 * 错误码，SDcard不存在 
	 */
	public static final int ERROR_NO_SDCARD = 4;
	
	/**
	 * 空闲状态
	 */
	public static final int STATE_IDLE = 0;
	/**
	 * 准备状态
	 */
	public static final int STATE_PREPARE = 1;
	
	/**
	 * 开始下载
	 */
	public static final int STATE_START = 2;
	
	/**
	 * 下载中
	 */
	public static final int STATE_DOWNLOADING = 3;
	
	/**
	 * 下载成功
	 */
	public static final int STATE_COMPLETE = 4;
	
	/**
	 * 下载出错
	 */
	public static final int STATE_ERROR = 5;
	
	/**
	 * 停止下载
	 */
	private final int STATE_STOP = 6;
	
	private final String TAG = "DownloadTaskManager";
	
	private String sdcardPath = Environment.getExternalStorageDirectory().getAbsolutePath();
	
	private OnDownloadListener listener;
	
	private DownloadThread downloadTask;
	
	private LinkedList<DownloadBean> mDownloadList;
	
	private boolean isDownload;
	
	private int tryDownloadCount = 1;
	
	private int currentDownloadCount = 0;
	
	private DownloadState mCurrentDownloadState;
	
	private DownloadBean mCurrentDownloadBean;
	
	private IConnectionBuilder mConnectionBuilder;     //自定义下载连接协议
	
	private boolean mIgnoreErrorDataResult = false;    //是否忽略数据类型错误，互传通讯协议返回 "Content-Type 为 null"，但是不认为是错误
	
	private boolean mDeleteFileWhenDownloadError = true;   // 是否在下载出错后删除文件 
	
	private final String THUMB_MEDIA_CACHE_PATH = "/qvod/.thumb/media/";
	
	public DownloadTaskManager() {
		mDownloadList = new LinkedList<DownloadBean>();
	}
	
	public void setOnDownloadListener(OnDownloadListener listener) {
		this.listener = listener;
	}
	
	public void setTryDownloadCount(int count) {
		this.tryDownloadCount = count;
	}
	
	/**
	 * 是否在下载出错后删除文件 
	 */
	public void setDeleteFileWhenDownloadError(boolean delete){
	    mDeleteFileWhenDownloadError = delete;
	}
	
	/**
	 * 是否忽略数据类型错误，互传通讯协议返回 "Content-Type 为 null"，但是不认为是错误
	 */
	public void setIgnoreErrorDataResult(boolean ignore){
	    mIgnoreErrorDataResult = ignore;
	}
	
	/**
	 * 返回所有下载任务
	 */
	public List<DownloadBean> getAllDownloads(){
	    return mDownloadList;
	}
	
	public boolean addDownload(DownloadBean downloadBean) {
		String downloadUrl = downloadBean.downloadUrl;
		if (downloadUrl == null || "".equals(downloadUrl)) {
			return false;
		}
		synchronized (mDownloadList) {
			mDownloadList.add(downloadBean);
		}
		return true;
	}
	
	public boolean addFristDownload(DownloadBean downloadBean) {
		String downloadUrl = downloadBean.downloadUrl;
		if (downloadUrl == null || "".equals(downloadUrl)) {
			return false;
		}
		synchronized (mDownloadList) {
			mDownloadList.remove(downloadBean);
			mDownloadList.addFirst(downloadBean);
		}
		return true;
	}
	
	/**
	 * [下载文件]<br/>
	 * @param connectionBuilder 提供Http连接，null表示采用默认的连接，自定义协议传入自己的connection (mod by LiuXiaoyuan)
	 * @return
	 */
	public boolean startDownload(Context context, IConnectionBuilder connectionBuilder) {
		if (context == null) {
			Log.e(TAG, "Context 不能为空");
			return false;
		}
	    mConnectionBuilder = connectionBuilder;
		isDownload = true;
		if (downloadTask == null) {
			downloadTask = new DownloadThread(context);
			downloadTask.start();
		} else {
			Log.i(TAG, "正在下载中");
		}
		return true;
	}
	
	/**
	 * [获取下载状态]<br/>
	 * 状态包括：准备、开始、下载中、完成、停止、错误
	 * @return
	 */
	public int getCurrentDownloadState() {
		if (mCurrentDownloadState != null) {
			return STATE_IDLE;
		}
		return mCurrentDownloadState.downloadState;
	}
	
	public DownloadBean getCurrentDownlaod() {
		return mCurrentDownloadBean;
	}
	
	/**
	 * [停止下载]<br/>
	 * 下载状态为准备、开始、下载中的情况下才可以停止
	 */
	public synchronized void stopDownload() {
		isDownload = false;
		if(downloadTask != null){
		    downloadTask.interrupt();
		    downloadTask = null;
		}
		synchronized (mDownloadList) {
			mDownloadList.clear();
		}
	}
	
	/**
	 * [是否已经停止]<br/>
	 * @return
	 */
	public boolean isStopDownload() {
		return isDownload;
	}
	
	public int getDownloadTaskSize() {
		synchronized (mDownloadList) {
			return mDownloadList.size();
		}
	}
	
	class DownloadThread extends Thread {
		Context context;
		
		DownloadThread(Context context) {
			this.context = context;
		}
		
		public void run() {
			while(isDownload) {
				currentDownloadCount = 0;
				
				DownloadBean downloadBean = null;
				synchronized (mDownloadList) {
					if (! mDownloadList.isEmpty()) {
						downloadBean = mDownloadList.getFirst();
					}
				}
				
				if (downloadBean != null) {
					mCurrentDownloadBean = downloadBean;
					DownloadState state = new DownloadState();
					state.downloadState = STATE_PREPARE;
					mCurrentDownloadState = state;
					if (listener != null) {
						listener.onDownloadPrepare(downloadBean);
					}
					
					repeatDownload(context, downloadBean, state);
					
					mCurrentDownloadState = null;
					mCurrentDownloadBean = null;
				}
				
				synchronized (mDownloadList) {
					if (downloadBean != null) {
						mDownloadList.remove(downloadBean);
					}
					if (mDownloadList.isEmpty()) {
						downloadTask = null;
						break;
					}
				}
			}
		}
	}

	/**
	 * @param connection Http连接，null表示采用默认的连接，自定义协议传入自己的connection (mod by LiuXiaoyuan)
	 */
	private void repeatDownload(Context context, DownloadBean downloadBean, DownloadState state) {
		boolean isSuccess = download(context, downloadBean, state);
		if (! isSuccess) {
			if (currentDownloadCount < tryDownloadCount) {
				repeatDownload(context, downloadBean, state);
			}
		}
	}
	
	/**
	 * [下载文件]<br/>
	 */
	private boolean download(Context context, DownloadBean downloadBean, DownloadState state){
		if (context == null) {
			return false;
		}
		
 		currentDownloadCount++;
		if (downloadBean.checkNetwork && !NetWorkUtils.isNetworkAvailable(context)) {
			state.downloadState = STATE_ERROR;
			state.errorCode = ERROR_CANNOT_CONNECT;
			publishDownloadState(state, downloadBean);
			return false;
		}
		
		if (! isDownload) {
			state.downloadState = STATE_STOP;
			state.isDownloadResult = true;
			publishDownloadState(state, downloadBean);
			return false;
		}
		Log.i(TAG, "开始下载,第 " + currentDownloadCount + " 次");
		
		BufferedInputStream bufferedInputStream = null;
	//	FileOutputStream fileOutputStream = null;
		RandomAccessFile writeToFile = null;
		HttpURLConnection conn = null;
		String downloadUrl = downloadBean.downloadUrl;
		try {
			//网络连接请求
			URL url = new URL(downloadUrl);
			//conn = WebUtils.getConnection(url, WebUtils.METHOD_GET, null);
			if(mConnectionBuilder != null){
			    conn = mConnectionBuilder.getConnection(url, downloadBean.tag);
			}else{
			    conn = WebUtils.getConnection(url, WebUtils.METHOD_GET, null);
			}
			Log.i(TAG, "begin get Rsp code ");
			int responseCode = conn.getResponseCode();
			Log.d(TAG,"rsp code:" + responseCode);
			if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
				state.errorCode = responseCode;
				state.downloadState = STATE_ERROR;
				state.isDownloadResult = true;
				publishDownloadState(state, downloadBean);
				Log.e(TAG, "网络请求错误：" + responseCode + " - URL: " + downloadUrl);
				return false;
			}

			//使用CMCC网络时，没有权限上网的情况下，服务器会返回HTML文本
			String contentType = conn.getHeaderField("Content-Type");
			Log.v(TAG, "contentType:" + contentType);
			if(!mIgnoreErrorDataResult){
			    // 互传不考虑这个错误
    			if (contentType == null || contentType.contains("text/html")) {
    				state.downloadState = STATE_ERROR;
    				state.errorCode = ERROR_RESULT;
    				state.isDownloadResult = true;
    				publishDownloadState(state, downloadBean);
    				return false;
    			}
			}
			String filePath = downloadBean.savePath;
			if (filePath == null) {
				int index = downloadUrl.lastIndexOf("/") + 1;
				String fileName = downloadUrl.substring(index, downloadUrl.length());
				String sdcardPath = SdcardUtil.getSDPath();
				if (sdcardPath == null) {
					state.downloadState = STATE_ERROR;
					state.errorCode = ERROR_NO_SDCARD;
					state.isDownloadResult = true;
					publishDownloadState(state, downloadBean);
					return false;
				}
				String mediaCachePath = sdcardPath + THUMB_MEDIA_CACHE_PATH;
				filePath = mediaCachePath + fileName;
			}
			File file = new File(filePath);
			
			if (file.exists()) {
			    if(downloadBean.downloadOffset == 0 || downloadBean.downloadOffset != file.length()){
			        // 如果不是断点续传，或者断点续传的大小对不上，那么删除文件，重新下载
			        file.delete();
			        downloadBean.downloadOffset = 0; //确保从零开始下
			    }
			}
			
			//容量不足
			long size = conn.getContentLength();
			// 手机断点续传返回的长度为剩余长度，所有总长度应该是剩余长度加已有长度
			long availableSpace = SdcardUtil.getAvailaleSize(sdcardPath);
			if(size >= availableSpace){
				state.downloadState = STATE_ERROR;
				state.errorCode = ERROR_NO_SPACES;
				state.isDownloadResult = true;
				publishDownloadState(state, downloadBean);
				return false;
			}
			size = size + downloadBean.downloadOffset;
			
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			
			if(!file.exists()){
			    file.createNewFile();
			}
			
			if (! isDownload) {
				//停止下载
				state.downloadState = STATE_STOP;
				state.isDownloadResult = true;
				publishDownloadState(state, downloadBean);
				return false;
			}
			
			//开始下载
			state.downloadState = STATE_START;
			publishDownloadState(state, downloadBean);
			
			//这之后出错则会尝试删除文件
			state.isCreateFile = true;
			
			byte[] buffer = new byte[1024 * 4];
			bufferedInputStream = new BufferedInputStream(conn.getInputStream());
			//fileOutputStream = new FileOutputStream(filePath);
			writeToFile = new RandomAccessFile(filePath, "rw");
			int len = 0;
			long hasRead = 0;  
			int progressOld = -1;
			writeToFile.seek(downloadBean.downloadOffset);
			hasRead += downloadBean.downloadOffset;
			while ((len = bufferedInputStream.read(buffer)) != -1) {
				
				if (! isDownload) {
					//停止下载
					state.downloadState = STATE_STOP;
					state.isDownloadResult = true;
					publishDownloadState(state, downloadBean);
					return false;
				}
				
				if (state.downloadProgress == 0 && progressOld == -1) {
					state.downloadState = STATE_DOWNLOADING;
					publishDownloadState(state, downloadBean);
					progressOld = 0;
				}
				
				writeToFile.write(buffer, 0, len);
				hasRead += len;  
				state.downloadProgress = (int)((hasRead*100)/size);  
				downloadBean.downloadSize = hasRead;
				if (progressOld == state.downloadProgress) {
					continue;
				}
				
				progressOld = state.downloadProgress;
				Log.i(TAG, "下载进度为:" + progressOld);
				publishDownloadState(state, downloadBean);
			}
			
			//fileOutputStream.flush();
			buffer = null;
			
			//下载完成
			state.downloadState = STATE_COMPLETE;
			state.isDownloadResult = true;
			publishDownloadState(state, downloadBean);
			return true;
			
		} catch (IOException e) {
			Log.d(TAG,"IOException e:" + e.getMessage());
			state.downloadState = STATE_ERROR;
			state.errorCode = ERROR_CANNOT_CONNECT;
			state.isDownloadResult = true;
			publishDownloadState(state, downloadBean);
			e.printStackTrace();
		}catch (Exception e) {
			Log.d(TAG,"Exception e:" + e.getMessage());
			state.downloadState = STATE_ERROR;
			state.errorCode = ERROR_CANNOT_CONNECT;
			state.isDownloadResult = true;
			publishDownloadState(state, downloadBean);
			e.printStackTrace();
		}finally {
			try {
			    if(null != writeToFile){
			        writeToFile.close();
			    }

				if (null != bufferedInputStream) {
					bufferedInputStream.close();
				}
				
				if (null != conn) {
					conn.disconnect();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		
		return false;
	}
		
	public void publishDownloadState(DownloadState state, DownloadBean downloadBean) {
		if (listener == null) {
			return;
		}
		
		if (state.isDownloadResult && state.downloadState != STATE_COMPLETE) {
			//失败则删除文件
			if (state.isCreateFile && mDeleteFileWhenDownloadError) {
				String filePath = downloadBean.savePath;
				File file = new File(filePath);
				if (file.exists()) {
					file.delete();
				}
				state.isCreateFile = false;
			}
		}
		
		switch (state.downloadState) {
			case STATE_START:
				listener.onDownloadStart(downloadBean);
				break;
			case STATE_DOWNLOADING:
				listener.onDownloadProgress(state.downloadProgress, downloadBean);
				break;
			case STATE_COMPLETE:
				listener.onDownloadSuccess(downloadBean);
				break;
			case STATE_STOP:
				listener.onDownloadStop(downloadBean);
				break;
			case STATE_ERROR:
				Log.e(TAG, "下载失败! code :" + state.errorCode);
				listener.onDownloadError(state.errorCode, downloadBean);
				break;
			default:
				break;
		}
	}
	
	public class DownloadState implements Serializable{
		private static final long serialVersionUID = 1L;
		int downloadState;
		int errorCode;
		int downloadProgress;
		boolean isDownloadResult;
		public boolean isCreateFile;
	}
	
	public interface OnDownloadListener {

		/**
		 * [下载失败]<br/>
		 * @param errorCode 错误码<br/>
		 * 		ERROR_NO_SPACE 容量不足<br/>
		 * 		ERROR_CANNOT_CONNECT 连接错误，Exception<br/>
		 * 		ERROR_RESULT 返回数据的数据类型错误<br/>
		 * 		以及400、404、500等Http返回码
		 */
		void onDownloadError(int errorCode, DownloadBean downloadBean);
		
		/**
		 * [下载成功]<br/>
		 */
		void onDownloadSuccess(DownloadBean downloadBean);
		
		/**
		 * [正在下载]<br/>
		 * @param progress 进度条
		 */
		void onDownloadProgress(int progress, DownloadBean downloadBean);
		
		/**
		 * [准备下载]<br/>
		 */
		void onDownloadPrepare(DownloadBean downloadBean);
		
		/**
		 * [开始下载]<br/>
		 */
		void onDownloadStart(DownloadBean downloadBean);
		
		/**
		 * [停止下载]<br/>
		 */
		void onDownloadStop(DownloadBean downloadBean);
	}
	
	
	public static interface IConnectionBuilder{
	    public HttpURLConnection getConnection(URL url,Object transFileInfo);
	}
}

