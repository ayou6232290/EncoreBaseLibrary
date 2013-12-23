package cn.encore.lib.http;

import java.io.IOException;

/**
 * [请求Result异常]
 * 请求ResponseCode不为200的时候发此异常
 * @author Encore
 * @date 2012-7-16
 */
public class HttpConnectionResultException extends IOException{

	private int errorCode;
	
	public HttpConnectionResultException(int errorCode) {
		super(errorCode+"");
		this.errorCode = errorCode;
	}
	
	@Override
	public String toString() {
		return "请求错误,返回码" + super.getMessage();
	}
	
	public int getErrorCode() {
		return errorCode;
	}
}

