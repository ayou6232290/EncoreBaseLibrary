package cn.encore.lib.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cn.encore.lib.utils.Log;
import cn.encore.lib.utils.StringUtils;

/**
 * [发送请求处理类]
 * 执行doget或dopost方法
 * @author Encore.liang
 * @date 2012-7-16
 */
public class WebUtils {
	
	private static final String TAG = "WebUtils";
	
	private static final String DEFAULT_CHARSET = "utf-8";
	
	public static final String METHOD_POST = "POST";
	
	public static final String METHOD_GET = "GET";
	
	private static final int CONNECTION_TIMEOUT = 20*1000;
	
	public static String doPost(String url, Map<String, String> postParams, 
			Map<String, String> getParams, Map<String, String> httpHead) throws IOException {
		
		String param = buildQuery(postParams, DEFAULT_CHARSET, false);
		return doPost(url, param, getParams, httpHead);
	}
	
	public static String doPost(String url, Map<String, String> httpHead) throws IOException {
		
		return doPost(url, "", null, httpHead);
	}
	
	public static String doPost(String url, String postParams) throws IOException {
		
		return doPost(url, postParams, null, null);
	}
	
	public static String doPost(String url, String postParams, Map<String, String> getParams, Map<String, String> httpHead) throws IOException {
		return doPost(url, postParams, getParams, httpHead, CONNECTION_TIMEOUT);
	}
	
	public static String doPost(String url, String postParams, Map<String, String> getParams, 
			Map<String, String> httpHead, int timeOut) throws IOException {
		
		if (url == null) {
			return null;
		}
		
		if(getParams != null && !"".equals(getParams)){
			url = buildRequestUrl(url, getParams, true);
		}
		
		Log.i(TAG, "doPost:" + url);
		Log.i(TAG, "doPost - postParams:" + postParams);
		
		byte[] datas = null;
		if (postParams != null && !"".equals(postParams)) {
			datas = postParams.getBytes(DEFAULT_CHARSET);
		}
		return connection(url, METHOD_POST, datas, httpHead, timeOut);
	}

	/**
	 * doGet请求方法
	 * 
	 * @param url
	 * @return
	 * @throws IOException 
	 */
	public static String doGet(String url) throws IOException{
		return doGet(url, null);
	}

	public static String doGet(String url, Map<String, String> params) throws IOException {
		return doGet(url, params, null);
	}
	
	public static String doGet(String url, Map<String, String> params, Map<String, String> httpHead) throws IOException {
		return doGet(url, params, httpHead, CONNECTION_TIMEOUT);
	}
	
	/**
	 * doGet请求方法
	 * 
	 * @param url
	 * @param params
	 * @return
	 * @throws IOException
	 */
	public static String doGet(String url, Map<String, String> params, Map<String, String> httpHead, int timeOut) throws IOException {
		
		if (StringUtils.isEmpty(url)) {
			return null;
		}
		
		if (params != null) {
			url = buildRequestUrl(url, params, true);
		}
		
		Log.i(TAG, "doGet:" + url);
		
		return connection(url, METHOD_GET, null, httpHead, timeOut);
	}
	
	/**
	 * doPost请求方法
	 * 
	 * @param url
	 * @param ctype
	 * @param content
	 * @return
	 * @throws IOException
	 */
	public static String connection(String url, String method, byte[] postContent, Map<String, String> httpHead, int timeOut) throws IOException {
		
		HttpURLConnection conn = null;
		OutputStream out = null;
		String rsp = null;
		int responseCode = 0;
		try 
		{
			conn = getConnection(new URL(url), method, httpHead, timeOut);	
			
			if (method.equals(METHOD_POST) && postContent != null) {
				conn.setDoOutput(true);
				out = conn.getOutputStream();
				if (out != null) {
					out.write(postContent);
				}
			}
			
			responseCode = conn.getResponseCode();
			
			Log.i(TAG, "connection - responseCode: " + responseCode);
			
			if (responseCode == HttpURLConnection.HTTP_OK) {
				InputStream inputStream = conn.getInputStream();
				String charset = getResponseCharset(conn.getContentType());
				rsp = getStreamAsString(inputStream, charset);
				
				Log.v(TAG, "结果(" + charset + ")：" + rsp);
			} else {
				throw new HttpConnectionResultException(responseCode);
			}
			
		}catch(IOException e){
			Log.e(TAG, "connection - IOException:" + e.getLocalizedMessage() + " responseCode:" + responseCode);
			if(responseCode == 401){
				throw new HttpConnectionResultException(401);
			}else {
				throw new HttpConnectionResultException(500);
			}
		}
		finally {
			if (out != null) {
				out.close();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}

		return rsp;
	}
	
	public static HttpURLConnection getConnection(URL url, String method, Map<String, String> httpHead) throws IOException {
		return getConnection(url, method, httpHead, CONNECTION_TIMEOUT);
	}
	
	/**
	 * 得到Http连接
	 * 
	 * @param url
	 * @param method
	 * @param ctype
	 * @return
	 * @throws IOException
	 */
	public static HttpURLConnection getConnection(URL url, String method, Map<String, String> httpHead, int timeout) throws IOException {
		
		HttpURLConnection conn = null;
		
		conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(method);
		conn.setDoInput(true);
		conn.setConnectTimeout(timeout);
		conn.setReadTimeout(timeout);
		conn.setRequestProperty("Accept", "*/*");
		conn.setRequestProperty("User-Agent", "Apache-HttpClient/UNAVAILABLE (java 1.4)");
		conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + DEFAULT_CHARSET);
		conn.setRequestProperty("connection", "Keep-Alive");
		
		if (httpHead != null) {
			Iterator<String> it = httpHead.keySet().iterator();
			while(it.hasNext()) {
				String key = it.next();
				String value = httpHead.get(key);
				
				if (key != null && value != null) {
					conn.setRequestProperty(key, value);
				}
				
				Log.v(TAG, "Head:" + key + " - " + value);
			}
		}
		
		return conn;
	}
	
	public static String buildRequestUrl(String url, Map<String, String> params,  boolean hasSeparator) {
		try {
			String query = buildQuery(params, DEFAULT_CHARSET, false);
			return url = buildURl(url, query, hasSeparator);
		}catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * [URL参数拼接]<br/>
	 * @param strUrl
	 * @param query
	 * @return
	 * @throws MalformedURLException
	 */
	public static String buildURl(String strUrl, String query, boolean hasSeparator) throws MalformedURLException{
		URL url = new URL(strUrl);
		if (StringUtils.isEmpty(query)) {
			return strUrl;
		}

		if (StringUtils.isEmpty(url.getQuery())) {
			if (strUrl.endsWith("?")) {
				strUrl = strUrl + query;
			} else {
				if (hasSeparator) {
					if (strUrl.endsWith("/")) {
						strUrl = strUrl + "?" + query; 
					} else {
						strUrl = strUrl + "/?" + query; 
					}
				} else {
					strUrl = strUrl + "?" + query; 
				}
			}
		} else {
			if (strUrl.endsWith("&")) {
				strUrl = strUrl + query;
			} else {
				strUrl = strUrl + "&" + query;
			}
		}
		Log.v(TAG, "  ------------------> request url : " + strUrl);
		return strUrl;
	}

	/**
	 * 根据参数Map来动态构造请求参数
	 * @param params
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public static String buildQuery(Map<String, String> params, String charset, boolean isEncoder) throws IOException {
		if (params == null || params.isEmpty()) {
			return null;
		}

		StringBuilder query = new StringBuilder();
		Set<Entry<String, String>> entries = params.entrySet();
		boolean hasParam = false;

		for (Entry<String, String> entry : entries) {
			String name = entry.getKey();
			String value = entry.getValue();
			if (name == null || name.length() == 0 || value == null || value.length() == 0) {
				continue;
			}
			if (hasParam) {
				query.append("&");
			} else {
				hasParam = true;
			}

			query.append(name).append("=");
			if (isEncoder) {
				query.append(URLEncoder.encode(value, charset));
			} else {
				query.append(value);
			}
		}
		
		Log.v(TAG, "  ------------------> params list : " + query.toString()); 
		
		return query.toString();
	}

	/**
	 * 将流转换成字符
	 * @param stream
	 * @param charset
	 * @return
	 * @throws IOException
	 */ 
	public static String getStreamAsString(InputStream stream, String charset) throws IOException {
		
		String resultString = "";
		
		if(stream == null || charset == null){
			return null;
		}
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset));
			StringWriter writer = new StringWriter();
			char[] chars = new char[256];
			int count = 0;
			while ((count = reader.read(chars)) > 0) {
				writer.write(chars, 0, count);
			}
			
			resultString = writer.toString();
//			Log.v(TAG, "返回的结果: "+ resultString);
			return resultString;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally {
			if (stream != null) {
				
				stream.close();
			}
		}
	}

	/**
	 * 得到返回信息的编码
	 * @param ctype
	 * @return
	 */
	public static String getResponseCharset(String ctype) {
		String charset = DEFAULT_CHARSET;

		if (!StringUtils.isEmpty(ctype)) {
			String[] params = ctype.split(";");
			for (String param : params) {
				param = param.trim();
				if (param.startsWith("charset")) {
					String[] pair = param.split("=", 2);
					if (pair.length == 2) {
						if (!StringUtils.isEmpty(pair[1])) {
							charset = pair[1].trim();
						}
					}
					break;
				}
			}
		}

		return charset;
	}
}
