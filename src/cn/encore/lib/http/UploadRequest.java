package cn.encore.lib.http;

/**
 * @author Encore.liang
 * @date 2013-6-28
 */
public class UploadRequest extends Request {

	private String uploadFilePath;

	public String getUploadFilePath() {
		return uploadFilePath;
	}

	public void setUploadFilePath(String uploadFilePath) {
		this.uploadFilePath = uploadFilePath;
	}
	
}

