package cn.encore.lib.json;

import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;

/**
 * @author Encore.liang
 * @date 2011-9-6
 * @version V1.0
 * @description
 */
public class FastJsonUtils {

	private static final String TAG = "JacksonUtils";
	private static FastJsonUtils mJacksonUtils;

	// private ObjectMapper mObjectMapper;

	public FastJsonUtils() {
		// mObjectMapper = new ObjectMapper();
		// mObjectMapper.configure(org.codehaus.jackson.map.DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
		// false);
		// mObjectMapper.configure(org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_NULL_MAP_VALUES,
		// false);
		// mObjectMapper.configure(org.codehaus.jackson.map.SerializationConfig.Feature.WRITE_NULL_PROPERTIES,
		// false);
	}

	public static FastJsonUtils shareJacksonUtils() {

		if (mJacksonUtils == null) {
			mJacksonUtils = new FastJsonUtils();
		}
		return mJacksonUtils;
	}

	/**
	 * 使用： ArrayList<AccountBean> beanList = new ArrayList<AccountBean>();
	 * 
	 * AccountBean bean = new AccountBean(); bean.setAddress("a1");
	 * bean.setEmail("email1"); bean.setId(1111); bean.setName("name");
	 * beanList.add(bean);
	 * 
	 * AccountBean bean2 = new AccountBean(); bean2.setAddress("a2");
	 * bean2.setEmail("email2"); bean2.setId(2222); bean2.setName("name2");
	 * beanList.add(bean2);
	 * 
	 * String aString = JacksonUtils.parseObj2Json(beanList);
	 * 
	 * @param bean
	 * @return
	 * @throws
	 */
	public String parseObj2Json(Object bean) {
		if (bean == null) {
			return null;
		}
		// ByteArrayOutputStream out = new ByteArrayOutputStream();
		// JsonGenerator jsonGenerator = mObjectMapper.getJsonFactory().
		// createJsonGenerator(out, JsonEncoding.UTF8);
		// jsonGenerator.writeObject(bean);

		String jsonString = JSON.toJSONString(bean);

		return jsonString;
	}

	/**
	 * 使用： String json = "{\"list\":[" + "{\"" + "address\": \"address2\"," +
	 * "\"name\":\"haha2\"," + "\"id\":2," + "\"email\":\"email2\"" + "},"+ "{"
	 * + "\"address\":\"address\"," + "\"name\":\"haha\"," + "\"id\":1," +
	 * "\"email\":\"email\"" + "}" + "]}"; AccountBeanResult obj =
	 * JacksonUtils.parseJson2Obj(json, AccountBeanResult.class);
	 * 
	 * @param <T>
	 * @param jsonStr
	 * @param c
	 * @return
	 * @throws
	 */
	public <T> T parseJson2Obj(String jsonStr, Class<T> c) {
		if (jsonStr == null) {
			return null;
		}
		// try {
		// T obj = mObjectMapper.readValue(jsonStr, c);
		// return obj;
		// } catch (JsonParseException e) {
		// e.printStackTrace();
		// } catch (JsonMappingException e) {
		// e.printStackTrace();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		T t = null;
		try {
			t = JSON.parseObject(jsonStr, c);
			return t;
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	/**
	 * 使用： String json = "[" + "{\"" + "address\": \"address2\"," +
	 * "\"name\":\"haha2\"," + "\"id\":2," + "\"email\":\"email2\"" + "},"+ "{"
	 * + "\"address\":\"address\"," + "\"name\":\"haha\"," + "\"id\":1," +
	 * "\"email\":\"email\"" + "}" + "]"; ArrayList<AccountBean> beanList =
	 * JacksonUtils.parseJson2List(json, AccountBean.class);
	 * 
	 * @param <T>
	 * @param json
	 * @param c
	 * @return
	 * @throws
	 */
	public <T> ArrayList<T> parseJson2List(String json, Class<T> c) {
		if (json == null) {
			return null;
		}
		// try {
		// ArrayList<T> jsonlist = mObjectMapper.readValue(json,
		// TypeFactory.collectionType(ArrayList.class, c));
		//
		// return jsonlist;
		// } catch (Exception e) {
		// e.printStackTrace();
		// }

		List list = new ArrayList();
		try {
			list = JSON.parseArray(json, c);
			return (ArrayList<T>) list;
		} catch (Exception e) {
		}
		return null;
	}
}
