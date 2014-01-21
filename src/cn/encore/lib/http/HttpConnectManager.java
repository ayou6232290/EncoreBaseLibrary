package cn.encore.lib.http;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import android.content.Context;
import cn.encore.lib.utils.Log;
import cn.encore.lib.utils.NetWorkUtils;

/**
 * [Http网络请求管理器] 网络以队列进行请求
 * 
 * @author Encore.liang
 * @date 2012-7-16
 */
public class HttpConnectManager {

	private final String TAG = "HttpConnectManager";

	/**
	 * 网络请求连接池等待10秒
	 */
	private final long CONNECTER_KEEP_ALIVE_TIME = 10 * 1000;

	/**
	 * State包括以下错误码，也包括HTTP的返回错误码
	 */

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
	 * token 失效
	 */
	public static final int STATE_TOKEN_EXCEPTION = 401;

	/**
	 * 网络不可用
	 */
	public static final int STATE_NETWORD_UNSEARCHABLE = 3;

	private static HttpConnectManager instance;
	private static HttpConnectManager backgroundInstance;

	private LinkedList<Request> requestList;

	private Thread httpThread;

	private Object mThreadObj = new Object();

	private Context mContext;

	private HttpConnectManager(Context context) {
		if (context == null) {
			throw new RuntimeException("context不能为空");
		}

		requestList = new LinkedList<Request>();
		mContext = context;
	}

	public static HttpConnectManager getBackgroundInstance(Context context) {
		if (backgroundInstance == null) {
			backgroundInstance = new HttpConnectManager(context);
		}
		return backgroundInstance;
	}

	public static HttpConnectManager getInstance(Context context) {
		if (instance == null) {
			instance = new HttpConnectManager(context);
		}
		return instance;
	}

	public static void release() {
		instance = null;
	}

	/**
	 * [Get请求]<br/>
	 * 
	 * @param request
	 *            请求参数
	 */
	public boolean doGet(Request request) {
		if (request == null) {
			return false;
		}
		request.setHttpType(Request.HTTP_TYPE_GET);
		return connection(request);
	}

	/**
	 * [Post请求]<br/>
	 * 
	 * @param request
	 *            请求参数
	 */
	public boolean doPost(Request request) {

		if (request == null) {
			return false;
		}
		return connection(request);
	}

	/**
	 * [Post请求]<br/>
	 * 
	 * @param request
	 *            请求参数
	 * @param postParam
	 *            Post参数
	 */
	public boolean doPost(Request request, Map<String, String> postParam) {

		if (request == null) {
			return false;
		}
		request.setHttpType(Request.HTTP_TYPE_POST);
		request.setPostDataType(Request.DATA_MAP);
		request.setPostData(postParam);

		return connection(request);
	}

	/**
	 * [Post请求]<br/>
	 * 
	 * @param request
	 *            请求参数
	 * @param postParam
	 *            Post参数
	 */
	public boolean doPost(Request request, String postParam) {

		if (request == null) {
			return false;
		}
		request.setHttpType(Request.HTTP_TYPE_POST);
		request.setPostDataType(Request.DATE_STRING);
		request.setPostData(postParam);

		return connection(request);
	}

	/**
	 * [添加网络请求到请求队列]<br/>
	 * 
	 * @param request
	 */
	private boolean connection(Request request) {

		if (request == null) {
			return false;
		}
		Context context = mContext;
		if (context == null) {
			Log.e(TAG, "connection - Context 为空");
			return false;
		}
		if (!NetWorkUtils.isNetworkAvailable(context)) {
			Log.e(TAG, "connection - 网络不可用");
			return false;

		}

		synchronized (requestList) {
			requestList.addLast(request);
		}

		if (httpThread == null) {
			// 启动连接管理器
			httpThread = new ConnectroThread();
			httpThread.start();
		} else {
			awakeDownload();
		}
		return true;
	}

	/**
	 * [唤醒下载线程]<br/>
	 */
	public void awakeDownload() {
		// 唤醒连接管理器
		if (httpThread == null) {
			return;
		}
		State state = null;
		try {
			state = httpThread.getState();
		} catch (Exception e) {
			state = state.BLOCKED;
			e.printStackTrace();
		}
		if (state == State.TIMED_WAITING || state == State.BLOCKED) {
			try {
				httpThread.interrupt();
			} catch (Exception e) {
			}
		}
	}

	public void clearRequest() {
		synchronized (requestList) {
			requestList.clear();
		}
	}

	/**
	 * [网络请求线程]<br/>
	 * 
	 * @author 李理
	 * @date 2012-7-18
	 */
	class ConnectroThread extends Thread {

		public void run() {
			while (true) {
				if (requestList.isEmpty()) {
					// 线程等待处理
					try {
						Log.e(TAG, "数据连接池等待中.....");
						sleep(CONNECTER_KEEP_ALIVE_TIME);
					} catch (Exception e) {
					}
				}

				Request request = null;
				synchronized (requestList) {
					if (requestList.isEmpty()) {
						Log.i(TAG, "关闭数据连接池");
						httpThread = null;
						break;
					} else {
						Log.i(TAG, "连接池Size：" + requestList.size());
						request = requestList.removeFirst();
						// request = getRequest();
					}
				}
				if (request == null) {
					continue;
				}
				connection(request);
			}
		}

		private Request getRequest() {
			Request request = null;
			Iterator<Request> it = requestList.iterator();
			while (it.hasNext()) {
				Request r = it.next();
				if (request == null) {
					request = r;
					continue;
				}
				if (r.getPriority() > request.getPriority()) {
					request = r;
				}
			}
			requestList.remove(request);
			return request;
		}

		private void connection(Request request) {
			Object result = null;
			int state = STATE_SUC;
			String url = request.getUrl();
			Log.i(TAG, "连接器发出请求：" + url);

			Context context = mContext;
			if (context == null) {
				return;
			}
			if (!NetWorkUtils.isNetworkAvailable(context)) {
				Log.e(TAG, "connection - 网络不可用，抛弃请求");
				state = STATE_NETWORD_UNSEARCHABLE;
				OnRequestListener listener = request.getOnRequestListener();
				if (listener != null) {
					listener.onResponse(url, state, null, request.getRequestType());
				}
				return;
			}

			try {
				Map<String, String> uriParam = request.getUriParam();
				Map<String, String> httpHead = request.getHttpHead();

				if (request.getHttpType() == Request.HTTP_TYPE_POST) {
					// 进行Post请求
					Object postParam = request.getPostData();
					if (postParam != null) {
						int dataType = request.getPostDataType();
						switch (dataType) {
						case Request.DATE_STRING:
							result = WebUtils.doPost(url, postParam.toString(), uriParam, httpHead);
							break;
						case Request.DATA_MAP:
							result = WebUtils.doPost(url, (Map<String, String>) postParam, uriParam, httpHead);
							break;
						default:
							result = "error";
							state = STATE_CALL_ERROR;
							break;
						}
					} else {
						result = WebUtils.doPost(url, httpHead);
					}
				} else {
					// 进行Get请求
					result = WebUtils.doGet(url, uriParam, httpHead);
				}
			} catch (HttpConnectionResultException e) {
				// Http Result不为200
				result = e.getErrorCode();
				if(e.getErrorCode() == 401){
					state = STATE_TOKEN_EXCEPTION;
				}else{
					state = Integer.parseInt(result.toString());
				}
				
				Log.i(TAG, "HttpConnectionResultException:"+ state);
			} catch (IOException e) {
				result = "connection error";
				state = STATE_TIME_OUT;
				e.printStackTrace();
				Log.i(TAG, "IOException:"+ state);
			} catch (Exception e) {
				e.printStackTrace();
				result = e.getMessage() + "";
				state = STATE_EXCEPTION;
				Log.i(TAG, "IOException:"+ state);
			}

			if (result == null) {
				state = STATE_EXCEPTION;
			}

			Log.i(TAG, "连接结束：" + url + " - state:" + state);
			if (state == STATE_SUC) {
				IDataParser parser = request.getParser();
				int code = 200;
				if (result instanceof Integer) {
					code = (Integer) result;
				}
				if (parser != null && code != 401) {
					result = parser.parseData(result.toString());
				}
			}

			OnRequestListener listener = request.getOnRequestListener();
			if (listener != null) {
				listener.onResponse(url, state, result, request.getRequestType());
			}
		}
	}

	public void onDestroy() {

		if (requestList != null) {
			synchronized (requestList) {
				requestList.clear();
			}
		}
		awakeDownload();
		instance = null;
	}
}
