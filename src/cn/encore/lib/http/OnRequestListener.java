package cn.encore.lib.http;

/**
 * [请求监听]
 * @author Encore.liang
 * @date 2012-7-16
 */
public interface OnRequestListener {
	
	/**
	 * [Http请求返回响应]<br/>
	 * @param url 请求Url<br/>
	 * @param state 请求状态<br/>
	 * 		STATE_CALL_ERROR 调用错误<br/>
	 * 		STATE_EXCEPTION 崩溃异常<br/>
	 * 		STATE_SUC 请求成功<br/>
	 * 		STATE_TIME_OUT 请求超时<br/>
	 * 		STATE_ERROR_RESULT Http返回码不为200<br/>
	 * @param result 返回数据
	 * @param type	请求类型，与发送请求时候传输的type一样
	 */
	void onResponse(String url, int state, Object result, int type);
}

