package cn.encore.lib.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.text.TextUtils;

/**
 * [字符处理工具类]
 */
public class StringUtils {

	private final static String TAG = "StringUtils";
	public static final String STAT_FORMAT = "yyyy-MM-dd HH";
	public static final String TIME_FORMAT = "H:mm:ss";
	public static final String DATE_FORMAT_CH = "yyyy年M月d日";
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String AM_PM_TIME_FORMAT = "HH:mm";
	public static final String HTTP = "http://";
	public static final String HTTPS = "https://";
	public static final String WWW_LOW = "www.";
	public static final String WWW_UPPER = "WWW.";
	public static final String FAVICON_URL_SUFFIX = "/favicon.ico"; 

	public static String OR(byte[] source, String key) {
		return OR(source, key, null);
	}

	public static byte[] OR2Byte(byte[] source, String key) {
		if (source == null || source.length < 1 || key == null || key.length() < 1) {
			return null;
		}
		final byte[] sourceBuff = source;
		byte[] keyBuff = key.getBytes();
		int keyLength = keyBuff.length;
		int keyIndex = 0;
		Integer byteResult = 0;

		byte[] result = new byte[sourceBuff.length];

		for (int i = 0; i < sourceBuff.length; i++) {
			keyIndex = i % keyLength;
			byteResult = sourceBuff[i] ^ keyBuff[keyIndex];
			result[i] = byteResult.byteValue();
		}

		return result;
	}

	public static String OR(byte[] source, String key, String charsetName) {
		byte[] result = OR2Byte(source, key);
		if (result == null) {
			return null;
		}

		String resultString = null;
		if (charsetName != null && !charsetName.equals("")) {
			try {
				resultString = new String(result, charsetName);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		} else {
			resultString = new String(result);
		}
		return resultString;
	}

	public static final boolean isHttpUrl(String url) {
//		String regEx = "^(https://|http://){0,1}([a-zA-Z0-9\\-]{1,}\\.){0,2}([a-zA-Z0-9\\-]{1,}\\.[a-zA-Z0-9]{1,})/{0,1}$";
		String regEx = "^(https://|http://){0,1}([a-zA-Z0-9]{1,}[a-zA-Z0-9\\-]{0,}\\.){0,4}([a-zA-Z0-9]{1,}[a-zA-Z0-9\\-]{0,}\\.[a-zA-Z0-9]{1,})/{0,1}$";
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(url);
		return m.find();
	}
	
	public static final String removeWWWHead(String url) {
		if (url == null) {
			return null;
		}

		String temp = url.toLowerCase();
		if (temp.startsWith(WWW_LOW)) {
			url = url.substring(WWW_LOW.length());
		} 
		
		return url;
	}

	public static final String removeHttpHead(String url) {
		if (url == null) {
			return null;
		}

		if (url.startsWith(HTTP)) {
			url = url.substring(HTTP.length());
		} else if (url.startsWith(HTTPS)) {
			url = url.substring(HTTPS.length());
		}

		return url;
	}
	
	public static final String getFaviconUrl(String url){
		String domainUrl = getDomainName(url, true);
		if(domainUrl != null){
			return domainUrl + FAVICON_URL_SUFFIX;
		}
		return null;
	}

	public static final String getDomainName(String url) {
		return getDomainName(url, false);
	}
	
	public static final String getDomainName(String url, boolean hasHttpHead) {
		if (url == null) {
			return null;
		}

		String result = url;
		int index;
		if (hasHttpHead) {
			if (url.indexOf(HTTP) == 0) {
				index = url.indexOf("/", HTTP.length());
			} else if (url.indexOf(HTTPS) == 0) {
				index = url.indexOf("/", HTTPS.length());
			} else {
				index = url.indexOf("/");
			}
		} else {
			result = removeHttpHead(url);
			index = result.indexOf("/");
		}
		if (index > -1) {
			result = result.substring(0, index);
		}
		return result;
	}
	
	public static String formatStatDate(){
		SimpleDateFormat format = new SimpleDateFormat(STAT_FORMAT);
		return format.format(new Date());
	}

	public static String formatString(Date date, String formatString) {
		String nowdate = "";
		if (date != null) {
			SimpleDateFormat format = new SimpleDateFormat(formatString);
			nowdate = format.format(date);
		}

		return nowdate;
	}

	public static String formatString(Date date) {
		return formatString(date, DATE_FORMAT_CH);
	}

	public static Date parseDate(String dateStr, String formatString) {
		Date date = null;
		if (dateStr != null && !dateStr.equals("")) {

			try {
				SimpleDateFormat format = new SimpleDateFormat(formatString);
				date = format.parse(dateStr);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		return date;
	}
	
	public static String getToday(String format){
		Calendar now = Calendar.getInstance();
		if (format != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(format);
			return dateFormat.format(now.getTime());
		} else {
			return now.getTime().getTime() + "";
		}
	}

	public static boolean isNumber(String str) {
		if (str == null || str.length() < 1) {
			return false;
		}

		Pattern pattern = Pattern.compile("[0-9]*.[0-9]*");
		return pattern.matcher(str).matches();
	}

	/**
	 * 是否为空
	 * 
	 * @param value
	 * @return
	 */
	public static boolean isEmpty(String value) {
		int strLen;
		if (value == null || (strLen = value.length()) == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if ((Character.isWhitespace(value.charAt(i)) == false)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 是否为邮件地址
	 */
	public static boolean isVaildEmail(String email) {
		if (email == null || email.trim().length() == 0) {
			return false;
		}
		String emailPattern = "[a-zA-Z0-9_-|\\.]+@[a-zA-Z0-9_-]+.[a-zA-Z0-9_.-]+";
		boolean result = Pattern.matches(emailPattern, email);
		return result;
	}

	public static String parseDuration(int second) {
		if (second < 0) {
			return "";
		}
		SimpleDateFormat format = new SimpleDateFormat(TIME_FORMAT);
		format.setTimeZone(TimeZone.getTimeZone("GMT +08:00, GMT +0800"));
		return format.format(new Date(second));
	}

	public static int getFrontSynIndex(String text, int currentIndex) {
		if (text != null) {
			if (currentIndex >= 0 && currentIndex < text.length()) {
				char[] textArray = text.toCharArray();
				for (int i = currentIndex; i >= 0; i--) {
					if (textArray[i] == '.') {
						return i + 1;
					}
				}
			}
		}
		return 0;
	}

	public static int getBehindSynIndex(String text, int currentIndex) {
		if (text != null) {
			if (currentIndex >= 0 && currentIndex < text.length()) {
				char[] textArray = text.toCharArray();
				for (int i = currentIndex; i < text.length(); i++) {
					if (textArray[i] == '.') {
						return i;
					}
				}
				return text.length();
			}
		}
		return 0;
	}

	public static String parseDateFromInt(long date) {
		SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return myFormatter.format(date);
		} catch (Exception e) {
			return "";
		}
	}

	public static String parseDateIntoNumStr(long date) {
		SimpleDateFormat myFormatter = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		try {
			return myFormatter.format(date);
		} catch (Exception e) {
			return "";
		}
	}

	public static String parseDateFromInt(long date, String formatString) {
		SimpleDateFormat myFormatter = new SimpleDateFormat(formatString);
		try {
			return myFormatter.format(date);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 将从0开始的毫秒数转换为hh:mm:ss格式 如果没有hh那么转换为 mm:ss
	 * 
	 * @param milliseconds
	 * @return
	 */
	public static String parseZeroBaseMilliseconds(int milliseconds) {
		int hh = milliseconds / 1000 / 60 / 60;
		int mm = (milliseconds - (hh * 60 * 60 * 1000)) / 1000 / 60;
		int ss = (milliseconds - (mm * 1000 * 60) - (hh * 60 * 60 * 1000)) / 1000;
		if (hh != 0) {
			return String.format("%02d:%02d:%02d", hh, mm, ss);
		}
		return String.format("%02d:%02d", mm, ss);
	}

	/** int ip地址 to String 地址 */
	public static String intToIp(int i) {
		StringBuilder sb = new StringBuilder();
		sb.append(i & 0xFF);
		sb.append(".");
		sb.append((i >> 8) & 0xFF);
		sb.append(".");
		sb.append((i >> 16) & 0xFF);
		sb.append(".");
		sb.append((i >> 24) & 0xFF);
		return sb.toString();
	}

	public static final String[] filterStr = new String[] { ".com", ".cn", ".mobi", ".co", ".net", ".so", ".org",
			".gov", ".tel", ".tv", ".biz", ".cc", ".hk", ".name", ".info", ".asia", ".me", ".us" };

	private static String getFilterInfoPattern() {
		StringBuilder sb = new StringBuilder();
		sb.append("\\[.*?(");
		int size = filterStr.length;
		for (int i = 0; i < size; i++) {
			sb.append(filterStr[i]);
			if (i != size - 1) {
				sb.append("|");
			}
		}
		sb.append(")\\]");
		return sb.toString();
	}

	private static final String FILTER_INFO_PATTERN = getFilterInfoPattern();

	public static String filterInfo(String str) {
		if (str == null) {
			return null;
		}
		Pattern pattern = Pattern.compile(FILTER_INFO_PATTERN);
		Matcher matcher = pattern.matcher(str);
		return matcher.replaceAll("");
	}

	public static String formatChar(String source) {
		if (source != null) {
			Pattern p = Pattern.compile("[^a-zA-Z0-9]");
			return p.matcher(source).replaceAll("");
		}
		return null;
	}

	public static boolean isHexWepKey(String wepKey) {
		final int len = wepKey.length();
		// WEP-40, WEP-104, and some vendors using 256-bit WEP (WEP-232?)
		if (len != 10 && len != 26 && len != 58) {
			return false;
		}
		return isHex(wepKey);
	}

	public static boolean isHex(String key) {
		for (int i = key.length() - 1; i >= 0; i--) {
			final char c = key.charAt(i);
			if (!(c >= '0' && c <= '9' || c >= 'A' && c <= 'F' || c >= 'a' && c <= 'f')) {
				return false;
			}
		}
		return true;
	}

	public static String toHex(byte[] buf) {
		if (buf == null)
			return "";
		StringBuffer result = new StringBuffer(2 * buf.length);
		for (int i = 0; i < buf.length; i++) {
			appendHex(result, buf[i]);
		}
		return result.toString();
	}

	private final static String HEX = "0123456789ABCDEF";

	private static void appendHex(StringBuffer sb, byte b) {
		sb.append(HEX.charAt((b >> 4) & 0x0f)).append(HEX.charAt(b & 0x0f));
	}

	public static byte[] toByte(String hexString) {
		int len = hexString.length() / 2;
		byte[] result = new byte[len];
		for (int i = 0; i < len; i++)
			result[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
		return result;
	}

	public static String toQuotedString(String str) {
		if (TextUtils.isEmpty(str)) {
			return null;
		}
		final int lastPos = str.length() - 1;
		if (lastPos < 0 || (str.charAt(0) == '"' && str.charAt(lastPos) == '"')) {
			return str;
		}
		return "\"" + str + "\"";
	}

	public static String urlEncode(String url) {
		StringBuffer urlB = new StringBuffer();
		StringTokenizer st = new StringTokenizer(url, "/ ", true);
		while (st.hasMoreTokens()) {
			String tok = st.nextToken();
			if (tok.equals("/"))
				urlB.append("/");
			else if (tok.equals(" "))
				urlB.append("%20");
			else {
				try {
					urlB.append(URLEncoder.encode(tok, "UTF-8"));
				} catch (java.io.UnsupportedEncodingException uee) {

				}
			}
		}
		// Log.d(TAG, "urlEncode urlB:" + urlB.toString());
		return urlB.toString();
	}

	/**
	 * 将若干条字符串拼接成一条,多条之间的分隔符为分隔符连接
	 * 
	 * @param list
	 * @return
	 */
	public static List<String> composeStringList(List<String> list, int maxLength, String split) {
		if (list == null || list.size() <= 1 || split == null || maxLength <= 0) {
			return list;
		}

		ArrayList<String> comList = new ArrayList<String>();
		StringBuilder comString = new StringBuilder(list.get(0));

		for (int i = 1; i < list.size(); i++) {
			if ((comString.length() + list.get(i).length()) >= maxLength) {
				comList.add(comString.toString());
				comString = new StringBuilder(list.get(i));
			} else {
				comString.append(split).append(list.get(i));
			}
		}

		comList.add(comString.toString());
		return comList;
	}
}
