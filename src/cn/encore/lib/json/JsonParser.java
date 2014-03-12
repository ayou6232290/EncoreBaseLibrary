/*
 * 版    权： 深圳市快播科技有限公司
 * 描    述: 
 * 创建人: 李理
 * 创建时间: 2012-9-8
 */
package cn.encore.lib.json;

import cn.encore.lib.http.IDataParser;

/**
 * [一句话功能简述]<br/>
 * 
 * @author Encore.liang
 * @date 2012-9-8
 */
public class JsonParser implements IDataParser{

	private Class parserClass;
	/**
	 * 是否是列表
	 */
	private boolean isList;
	
	/**
	 * 
	 * @param parseClass 解析类
	 * @param isList 是否解析为列表
	 */
	public JsonParser(Class parseClass, boolean isList) {
		this.parserClass = parseClass;
		this.isList = isList;
	}
	
	public JsonParser(Class parseClass) {
		this(parseClass, false);
	}
	
	@Override
	public Object parseData(String data) {
		if(isList){
			return FastJsonUtils.shareJacksonUtils().parseJson2List(data, parserClass);
		}else{
			return FastJsonUtils.shareJacksonUtils().parseJson2Obj(data, parserClass);	
		}
	}
}

