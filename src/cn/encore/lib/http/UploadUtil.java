package cn.encore.lib.http;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import cn.encore.lib.utils.Log;
import cn.encore.lib.utils.NetWorkUtils;

/**
 * @author Encore.liang
 * @date 2013-6-28
 */
public class UploadUtil {
	
	private static final String TAG = "UploadUtil";

	private static final int CONNECTION_TIMEOUT = 40*1000;
	
	/**
	 * 调用错误
	 */
	public static final int STATE_CALL_ERROR = -1;
	
	/**
	 * 崩溃异常
	 */
	public static final int STATE_EXCEPTION = 0;
	
	/**
	 * 请求成功
	 */
	public static final int STATE_SUC = 1;
	
	/**
	 * 请求超时
	 */
	public static final int STATE_TIME_OUT = 2;
	
	/**
	 * 网络不可用
	 */
	public static final int STATE_NETWORD_UNSEARCHABLE = 3;
	
	public static void startUploadFile(Context context, UploadRequest request) {
		if (context == null || request == null) {
			return;
		}
		Thread thread = new UploadThread(context, request);
		thread.start();
	}
	
	static class UploadThread extends Thread {

		Context context;
		UploadRequest request;
		
		UploadThread(Context context, UploadRequest request) {
			this.request = request;
			this.context = context;
		}
		
		public void run() {
			OnRequestListener listener = request.getOnRequestListener();
			String url = request.getUrl();
			String filePath = request.getUploadFilePath();
			Map<String, String> getParams = request.getUriParam();
			int state = STATE_SUC;
			Object result = null;
			if (! NetWorkUtils.isNetworkAvailable(context)) {
				Log.e(TAG, "connection - 网络不可用，抛弃请求");
				state = STATE_NETWORD_UNSEARCHABLE;
				if (listener != null) {
					listener.onResponse(url, state, null, 0);
				}
				return;
			}
			
			try 
			{
				Map<String, String> httpHead = new HashMap<String, String>();
				httpHead.put("Content-Type", "application/octet-stream");
				result = UploadUtil.uploadFile(url, filePath, httpHead, getParams);
			} catch (HttpConnectionResultException e) {
				//Http Result不为200
				result = e.getMessage();
				state = Integer.parseInt(result.toString());
				
			} catch (IOException e) {
				result = "connection error";
				state = STATE_TIME_OUT;
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
				result = e.getMessage() + "";
				state = STATE_EXCEPTION;
			}
			
			if (result == null) {
				state = STATE_EXCEPTION;
			}
			
			Log.i(TAG, "连接结束：" + url + " - state:" + state);
			if (state == STATE_SUC) {
				IDataParser parser = request.getParser();
				if (parser != null) {
					result = parser.parseData(result.toString());
				}
			}
			
			if (listener != null) {
				listener.onResponse(url, state, result, request.getRequestType());
			}
		}
	}
	
	public static String uploadFile(String url, String filePath, 
			Map<String, String> httpHead, Map<String, String> getParamMap) throws IOException {
		
		HttpURLConnection conn = null;
		OutputStream out = null;
		BufferedInputStream fileInputStream = null;
		String rsp = null;
		
		url = WebUtils.buildRequestUrl(url, getParamMap, true);
		
		Log.i(TAG, "uploadFile: " + url);
		try 
		{
			conn = WebUtils.getConnection(new URL(url), WebUtils.METHOD_POST, httpHead, CONNECTION_TIMEOUT);	
			
			conn.setDoOutput(true);
			out = conn.getOutputStream();
			if (out != null) {
				File file = new File(filePath);
				FileInputStream inputStream = new FileInputStream(file);
				fileInputStream = new BufferedInputStream(inputStream);
				byte[] buffer = new byte[1024];
				int readLen = 0;
				while((readLen = fileInputStream.read(buffer, 0, buffer.length)) != -1){
					out.write(buffer);
				}
				fileInputStream.close();
				fileInputStream = null;
			}
			
			int responseCode = conn.getResponseCode();
			
			Log.i(TAG, "connection - responseCode: " + responseCode);
			
			if (responseCode == HttpURLConnection.HTTP_OK) {
				InputStream inputStream = conn.getInputStream();
				String charset = WebUtils.getResponseCharset(conn.getContentType());
				rsp = WebUtils.getStreamAsString(inputStream, charset);
				
				Log.v(TAG, "结果(" + charset + ")：" + rsp);
			} else {
				throw new HttpConnectionResultException(responseCode);
			}
			
		}catch(IOException e){
			Log.e(TAG, "connection - IOException:" + e.getLocalizedMessage());
			throw e;
		}
		finally {
			if (out != null) {
				out.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
			if (fileInputStream != null) {
				fileInputStream.close();
			}
		}
		
		return rsp;
	}
}

