package cn.encore.lib.http;

import java.util.Map;

/**
 * [网络请求参数封装]
 * @author Encore.liang
 * @date 2012-7-16
 */
public class Request {

	/**
	 * Http类型，Post、Get
	 */
	private int httpType;
	
	public static final int HTTP_TYPE_GET = 0;
	
	public static final int HTTP_TYPE_POST = 1;
	
	/**
	 * Post参数类型
	 * String、Map
	 */
	private int postDataType;
	
	static final int DATE_NULL = -1;

	static final int DATE_STRING = 0;
	
	static final int DATA_MAP = 1;
	
	/**
	 * 请求URL
	 */
	private String url;
	
	/**
	 * Get请求参数
	 */
	private Map<String, String> uriParam;
	
	/**
	 * Http头信息
	 */
	private Map<String, String> httpHead;
	
	/**
	 * Http Post参数
	 */
	private Object postData;
	
	/**
	 * 请求类型，可以用于标示同一个URL请求处理不同事情
	 */
	private int requestType;
	
	/**
	 * 监听器
	 */
	private OnRequestListener listener;
	
	/**
	 * 数据解析器
	 */
	private IDataParser parser;
	 
	public static final int PRIOPITY_MAX = 10;
	public static final int PRIOPITY_HIGHER = 7;
	public static final int PRIOPITY_NORMAL = 5;
	public static final int PRIOPITY_LOWER = 3;
	public static final int PRIOPITY_MIN = 1;
	private int priority = PRIOPITY_NORMAL;
	
	public Request() {
	}
	
	public Request(String url) {
		this.url = url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getUrl() {
		return url;
	}

	public Map<String, String> getUriParam() {
		return uriParam;
	}

	public void setUriParam(Map<String, String> uriParam) {
		this.uriParam = uriParam;
	}

	Object getPostData() {
		return postData;
	}

	void setPostData(Object postData) {
		this.postData = postData;
	}

	public int getRequestType() {
		return requestType;
	}

	public void setRequestType(int requestType) {
		this.requestType = requestType;
	}

	int getHttpType() {
		return httpType;
	}

	void setHttpType(int httpType) {
		this.httpType = httpType;
	}

	int getPostDataType() {
		return postDataType;
	}

	void setPostDataType(int postDataType) {
		this.postDataType = postDataType;
	}

	public Map<String, String> getHttpHead() {
		return httpHead;
	}

	public void setHttpHead(Map<String, String> httpHead) {
		this.httpHead = httpHead;
	}
	
	public void setOnRequestListener(OnRequestListener l) {
		this.listener = l;
	}

	public OnRequestListener getOnRequestListener() {
		return listener;
	}

	public IDataParser getParser() {
		return parser;
	}

	public void setParser(IDataParser parser) {
		this.parser = parser;
	}
	
	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		if (priority > PRIOPITY_MAX || priority < PRIOPITY_MIN) {
			return;
		}
		this.priority = priority;
	}
}


