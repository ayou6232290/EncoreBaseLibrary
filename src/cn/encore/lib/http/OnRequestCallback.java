package cn.encore.lib.http;
/**
 * 请求,业务逻辑回调封装
 * @author Encore.liang
 *
 */
public interface OnRequestCallback {
	/**
	 * 请求成功
	 * @param result 最终结果,如果有设置json解析,会返回实体对象,没有则返回String字符串
	 */
	public void onSuccess(Object result);
	/**
	 * 请求超市
	 */
	public void onTimeOut();
	/**
	 * 请求失败
	 * @param msg
	 * @param state
	 */
	public void onFail(String msg, int state);
}
