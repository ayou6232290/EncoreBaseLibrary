package cn.encore.lib.download;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.os.AsyncTask;
import cn.encore.lib.http.WebUtils;
import cn.encore.lib.utils.Log;
import cn.encore.lib.utils.SdcardUtil;

/**
 * [文件下载工具]
 * @date 2012-7-17
 */
public class DownloadTask {

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
	 * 错误码
	 */
	private int errorCode = 0;
	
	/**
	 * 空闲状态
	 */
	private final int STATE_IDLE = 0;
	/**
	 * 准备状态
	 */
	private final int STATE_PREPARE = 1;
	
	/**
	 * 开始下载
	 */
	private final int STATE_START = 2;
	
	/**
	 * 下载中
	 */
	private final int STATE_DOWNLOADING = 3;
	
	/**
	 * 下载成功
	 */
	private final int STATE_COMPLETE = 4;
	
	/**
	 * 下载出错
	 */
	private final int STATE_ERROR = 5;
	
	/**
	 * 停止下载
	 */
	private final int STATE_STOP = 6;
	
	/**
	 * 下载状态
	 */
	private int downloadState = STATE_IDLE;
	
	private int downloadProgress = 0;
	
	private final String TAG = "DownloadTask";
	
	private String sdcardPath;
	
	private String downloadUrl;
	
	private String downloadPath;
	
	private OnDownloadListener listener;
	
	private DownloadAsyncTask downloadTask;
	
	private boolean isStopDownload;
	
	private boolean isCreateFile;
	
	private int tryDownloadCount = 1;
	
	private int currentDownloadCount = 0;
	
	public DownloadTask(String url, String downloadPath) {
		this.downloadUrl = url;
		this.downloadPath = downloadPath;
	}
	
	public void setOnDownloadListener(OnDownloadListener listener) {
		this.listener = listener;
	}
	
	public void setTryDownloadCount(int count) {
		this.tryDownloadCount = count;
	}
	
	/**
	 * [下载文件]<br/>
	 * @return
	 */
	public boolean startDownload() {
		if (downloadUrl == null || "".equals(downloadUrl)) {
			return false;
		}
		
		sdcardPath = SdcardUtil.getSDPath();
		if(sdcardPath == null){
			return false;
		}
		
		File file = new File(sdcardPath);
		if(!file.exists()){
			return false;
		}
		
		if (downloadTask == null) {
			reset();
			
			downloadTask = new DownloadAsyncTask();
			downloadTask.execute();
			
			return true;
		} else {
			Log.i(TAG, "正在下载中");
			return false;
		}
	}
	
	private void reset() {
		isCreateFile = false;
		isStopDownload = false;
		errorCode = 0;
		downloadState = STATE_IDLE;
		downloadProgress = 0;
		currentDownloadCount = 0;
	}
	
	/**
	 * [获取下载状态]<br/>
	 * 状态包括：准备、开始、下载中、完成、停止、错误
	 * @return
	 */
	public int getDownloadState() {
		return downloadState;
	}
	
	/**
	 * [停止下载]<br/>
	 * 下载状态为准备、开始、下载中的情况下才可以停止
	 */
	public void stopDownload() {
		if (downloadState == STATE_PREPARE || downloadState == STATE_START || downloadState == STATE_DOWNLOADING) {
			isStopDownload = true;
		}
	}
	
	/**
	 * [是否已经停止]<br/>
	 * @return
	 */
	public boolean isStopDownload() {
		return isStopDownload;
	}
	
	/**
	 * [异步下载具体实现类]<br/>
	 * 
	 * @date 2012-7-18
	 */
	class DownloadAsyncTask extends AsyncTask{
		
		@Override
		protected void onPreExecute() {
			
			downloadState = STATE_PREPARE;
			if (listener != null) {
				listener.onDownloadPrepare();
			}
		}

		@Override
		protected Object doInBackground(Object... params) {
			download();
			return null;
		}
		
		@Override
		protected void onProgressUpdate(Object... values) {
			
			if (listener == null) {
				return;
			}
			Log.i(TAG, "onProgressUpdate - downloadProgress:" + downloadProgress);
			switch (downloadState) {
				case STATE_START:
					listener.onDownloadStart();
					break;
				case STATE_DOWNLOADING:
					listener.onDownloadProgress(downloadProgress);
					break;
				default:
					break;
			}
		}
	
		@Override
		protected void onPostExecute(Object result) {
			
			downloadTask = null;
			if (listener == null) {
				return;
			}
			
			if (downloadState != STATE_COMPLETE) {
				//失败则删除文件
				if (isCreateFile) {
					String filePath = downloadPath;
					File file = new File(filePath);
					if (file.exists()) {
						file.delete();
					}
					isCreateFile = false;
				}
			}
			
			switch (downloadState) {
				case STATE_COMPLETE:
					listener.onDownloadSuccess();
					break;
				case STATE_STOP:
					listener.onDownloadStop();
					break;
				case STATE_ERROR:
					listener.onDownloadError(errorCode);
					break;
				default:
					break;
			}
		}
	
		/**
		 * [下载文件]<br/>
		 */
		private void download(){
			
			if (isStopDownload) {
				downloadState = STATE_STOP;
				return;
			}
			currentDownloadCount++;
			Log.i(TAG, "开始下载,第 " + currentDownloadCount + " 次");
			
			BufferedInputStream bufferedInputStream = null;
			FileOutputStream fileOutputStream = null;
			HttpURLConnection conn = null;
			
			try {
				//网络连接请求
				URL url = new URL(downloadUrl);
				conn = WebUtils.getConnection(url, WebUtils.METHOD_GET, null);
				
				int responseCode = conn.getResponseCode();
				if (responseCode != HttpURLConnection.HTTP_OK) {
					downloadState = STATE_ERROR;
					errorCode = responseCode;
					return;
				}

				//使用CMCC网络时，没有权限上网的情况下，服务器会返回HTML文本
				String contentType = conn.getHeaderField("Content-Type");
				Log.v(TAG, "contentType:" + contentType);
				
				if (contentType == null || contentType.contains("text/html")) {
					downloadState = STATE_ERROR;
					errorCode = ERROR_RESULT;
					return;
				}
				
				String filePath = downloadPath;
				File file = new File(filePath);
				if (file.exists()) {
					file.delete();
				}
				
				//容量不足
				long size = conn.getContentLength();
				long availableSpace = SdcardUtil.getAvailaleSize(sdcardPath);
				if(size >= availableSpace){
					downloadState = STATE_ERROR;
					errorCode = ERROR_NO_SPACES;
					return;
				}
				
				if (!file.getParentFile().exists()) {
					file.getParentFile().mkdirs();
				}
				
				if (isStopDownload) {
					//停止下载
					downloadState = STATE_STOP;
					return;
				}
				
				//开始下载
				downloadState = STATE_START;
				publishProgress();
				
				//这之后出错则会尝试删除文件
				isCreateFile = true;
				
				byte[] buffer = new byte[1024 * 4];
				bufferedInputStream = new BufferedInputStream(conn.getInputStream());
				fileOutputStream = new FileOutputStream(filePath);
				int len = 0;
				long hasRead = 0;  
				int progressOld = 0;
				
				while ((len = bufferedInputStream.read(buffer)) != -1) {
					
					if (isStopDownload) {
						//停止下载
						downloadState = STATE_STOP;
						return;
					}
					
					if (downloadProgress == 0) {
						downloadState = STATE_DOWNLOADING;
					}
					
					fileOutputStream.write(buffer, 0, len);
					hasRead += len;  
					downloadProgress = (int)((hasRead*100)/size);  
//					Log.v(TAG, "size:" + size + " - hasRead:" + hasRead + " - downloadProgress:" + downloadProgress);
						
					if (progressOld == downloadProgress) {
						continue;
					}
					
					progressOld = downloadProgress;
					publishProgress();
				}
				
				fileOutputStream.flush();
				buffer = null;
				
				//下载完成
				downloadState = STATE_COMPLETE;
				return;
				
			} catch (IOException e) {
				downloadState = STATE_ERROR;
				errorCode = ERROR_CANNOT_CONNECT;
				e.printStackTrace();
			}catch (Exception e) {
				downloadState = STATE_ERROR;
				errorCode = ERROR_CANNOT_CONNECT;
				e.printStackTrace();
			}finally {
				try {
					if (null != fileOutputStream) {
						fileOutputStream.close();
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
			
			if (currentDownloadCount < tryDownloadCount) {
				download();
			}
		}
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
		void onDownloadError(int errorCode);
		
		/**
		 * [下载成功]<br/>
		 */
		void onDownloadSuccess();
		
		/**
		 * [正在下载]<br/>
		 * @param progress 进度条
		 */
		void onDownloadProgress(int progress);
		
		/**
		 * [准备下载]<br/>
		 */
		void onDownloadPrepare();
		
		/**
		 * [开始下载]<br/>
		 */
		void onDownloadStart();
		
		/**
		 * [停止下载]<br/>
		 */
		void onDownloadStop();
	}
}


