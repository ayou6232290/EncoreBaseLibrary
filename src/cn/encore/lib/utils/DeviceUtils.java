/*
 * 文件名: DeviceTools.java
 * 版    权：深圳市快播科技有限公司
 * 描    述: 
 * 创建人: 胡启明
 * 创建时间:2012-7-16
 * 
 * 修改人：
 * 修改时间:
 * 修改内容：[修改内容]
 */
package cn.encore.lib.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.view.WindowManager;

/**
 * @author Encore.liang
 * @date 2012-7-16
 */
public class DeviceUtils {
	public static String TAG = "DeviceTools";
	public static final int OPERATOR_CHINA_MOBILE = 1;
	public static final int OPERATOR_CHINA_UNICOM = 2;
	public static final int OPERATOR_CHINA_TELECOM = 3;
	public static final int OPERATOR_UNKNOWN = 4;

	private static String CMD = "/system/bin/cat";
	private static String MAX_FREQUENCY = "/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq";

	/**
	 * 获取内存大小
	 * 
	 * @return
	 */
	public static long getRamInfo() {
		long total = 0;
		FileReader fr = null;
		BufferedReader br = null;

		try {
			fr = new FileReader("/proc/meminfo");
			br = new BufferedReader(fr);
			String line = br.readLine();
			line = line.replaceAll(" ", "").toLowerCase();
			int i = line.indexOf("memtotal:");
			int j = line.indexOf("kb");
			if (i > -1 && j > -1) {
				line = line.substring(i + "memtotal:".length(), j);
				total = Long.valueOf(line) / 1024;
			}
		} catch (Exception e) {
			// TODO: handle exception
		} finally {
			try {
				if (br != null) {
					br.close();
					br = null;
				}

				if (fr != null) {
					fr.close();
					fr = null;
				}
			} catch (Exception e) {

			}
		}

		return total;
	}

	public static boolean isGpsOpen(Context context) {
		if (context == null) {
			return false;
		}
		LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		// 获得手机是不是设置了GPS开启状态
		return lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
	}

	public static String getLocalIp(Context context) {
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo wi = wm.getConnectionInfo();
		int ipAdd = wi.getIpAddress();

		return (ipAdd & 0xFF) + "." + ((ipAdd >> 8) & 0xFF) + "." + ((ipAdd >> 16) & 0xFF) + "." + (ipAdd >> 24 & 0xFF);
	}

	public static String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String getLocalMacAddress(Context context) {
		WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (wifi != null) {
			WifiInfo info = wifi.getConnectionInfo();
			if (info != null) {
				String mac = info.getMacAddress();
				return StringUtils.formatChar(mac);
			}
		}
		return null;
	}

	public static int getOperator(Context context) {
		TelephonyManager tel = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if (tel != null) {
			String simOperator = tel.getSimOperator();
			if ("46000".equals(simOperator) || "46002".equals(simOperator)) {
				return OPERATOR_CHINA_MOBILE;
			} else if ("46001".equals(simOperator)) {
				return OPERATOR_CHINA_UNICOM;
			} else if ("46003".equals(simOperator)) {
				return OPERATOR_CHINA_TELECOM;
			}
		}

		return OPERATOR_UNKNOWN;
	}

	public static int getSdkInt() {
		return android.os.Build.VERSION.SDK_INT;
	}

	public static String getOsVersion() {
		return android.os.Build.VERSION.RELEASE;
	}

	public static String getModel() {
		return android.os.Build.MODEL;
	}

	public static String getBrand() {
		return android.os.Build.BRAND;
	}

	public static String getAppVersion(Context context) {
		String version = "";
		try {
			int verCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
			version = String.valueOf(verCode);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return version;
	}

	public static int getAppVersionCode(Context context) {
		int verCode = 0;
		try {
			verCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return verCode;
	}

	public static String getAppVersionName(Context context) {
		String version = "";
		try {
			version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return version;
	}

	/**
	 * [获取设备Rom大小]<br/>
	 * 功能详细描述
	 * 
	 * @return
	 */
	public static long getRomInfo() {
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long totalBlocks = stat.getBlockCount();
		long size = totalBlocks * blockSize;
		size /= 1024;
		size /= 1024;
		return size;
	}

	/**
	 * 
	 * [获取CPU频率]<br/>
	 * 功能详细描述
	 * 
	 * @return
	 */
	public static int getMaxCpuFreq() {
		//Log.d(TAG, "getMaxCpuFreq begin");
		int result = 0;
		ProcessBuilder cmd;
		try {
			String[] args = { CMD, MAX_FREQUENCY };
			cmd = new ProcessBuilder(args);
			Process process = cmd.start();
			InputStream in = process.getInputStream();
			byte[] buff = new byte[256];
			StringBuilder sb = new StringBuilder();
			while (in.read(buff) != -1) {
				sb.append(new String(buff));
			}
			in.close();
			String str = sb.toString();
			if (str != null) {
				str = str.trim();
				result = Integer.valueOf(str);
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		//Log.d(TAG, "getMaxCpuFreq end: " + result);
		return result;
	}

	

	/**
	 * 
	 * [获取是否是平板]<br/>
	 * 功能详细描述 定义960dp*720dp的设备为平板，其他为手机；
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isPad(Context context) {
		final int width = context.getResources().getDisplayMetrics().widthPixels;
		final int height = context.getResources().getDisplayMetrics().heightPixels;
		final float den = context.getResources().getDisplayMetrics().density;

		int w = (width > height) ? width : height;
		int h = (width < height) ? width : height;

		w = (int) (w / den);
		h = (int) (h / den);

		//Log.d(TAG, "width: " + width + " height: " + height + " den: " + den);
		return w > 960 && h > 720;
	}

	public static int getDeviceType(Context context) {
		// 1:手机 2:平板
		return isPad(context) ? 2 : 1;
	}

	public static boolean hasGoogleMapApi(Context context) {
		Object obj = null;
		try {
			obj = Class.forName("com.google.android.maps.MapActivity");
		} catch (Exception e) {
		}
		//Log.d(TAG, " obj: " + obj);
		return obj != null;
	}

	/**
	 * 判断是否开启了自动亮度调节
	 * 
	 * @param aContext
	 * @return
	 */
	public static boolean isAutoBrightness(Activity activity) {
		boolean automicBrightness = false;
		try {
			automicBrightness = Settings.System.getInt(activity.getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		return automicBrightness;
	}

	/**
	 * 停止自动亮度调节
	 * 
	 * @param activity
	 */
	public static void stopAutoBrightness(Activity activity) {
		Settings.System.putInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
				Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
	}

	/**
	 * 开启亮度自动调节
	 * 
	 * @param activity
	 */
	public static void startAutoBrightness(Activity activity) {
		Settings.System.putInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
				Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
	}

	/**
	 * 设置屏幕亮度
	 * 
	 * @param activity
	 * @param brightness
	 */
	public static void setBrightness(Activity activity, float brightness) {
		WindowManager.LayoutParams lp = activity.getWindow().getAttributes();
		lp.screenBrightness = brightness;
		activity.getWindow().setAttributes(lp);
	}

	/**
	 * 获取屏幕亮度
	 * 
	 * @param activity
	 * @param brightness
	 */
	public static float getBrightness(Activity activity) {
		float curBrightnessValue = 0.0f;
		try {
			float b = Settings.System.getInt(activity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
			// 需要转换成百分比
			curBrightnessValue = b / 255; 
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return curBrightnessValue;
	}

	/**
	 * 保持屏幕常亮
	 * 
	 * @param activity
	 */
	public static void setKeepScreenOn(Activity activity) {
		activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	 /** 
     * 收集设备参数信息 
     *  
     * @param context 
     */  
    public static Map<String, String> collectDeviceInfo(Context context) {  
    	if (context == null) {
    		return null;
    	}
    	Map<String, String> infoMap = new LinkedHashMap<String, String>();
        try {  
            PackageManager pm = context.getPackageManager();// 获得包管理器  
            //获取当前APP包的Version
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(),  
                    PackageManager.GET_ACTIVITIES); 
            if (pi != null) {  
                String versionName = pi.versionName == null ? "null"  
                        : pi.versionName;  
                String versionCode = pi.versionCode + "";  
                infoMap.put("versionName", versionName);  
                infoMap.put("versionCode", versionCode);  
            }  
        } catch (NameNotFoundException e) {  
            e.printStackTrace();  
        }  
        //获取设备信息
        Field[] fields = Build.class.getDeclaredFields();// 反射机制  
        for (Field field : fields) {  
            try {  
                field.setAccessible(true);  
                infoMap.put(field.getName(), field.get("").toString());  
//                Log.d(TAG, field.getName() + ":" + field.get(""));  
            } catch (IllegalArgumentException e) {  
                e.printStackTrace();  
            } catch (IllegalAccessException e) {  
                e.printStackTrace();  
            }  
        }  
        return infoMap;
    }  
    
    public static String getPhoneNumber(Context context){ 
        TelephonyManager telephonyMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);  
        return telephonyMgr.getLine1Number(); 
    }
    
    public static boolean hasSimCard(Context context) {
    	TelephonyManager mTelephonyManager = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE); 
         int simState = mTelephonyManager.getSimState(); 
         if (simState == TelephonyManager.SIM_STATE_ABSENT || simState == TelephonyManager.SIM_STATE_UNKNOWN) {
        	 return false;
         } else {
        	 return true;
         }
    }
    
	private static Object[] mArmArchitecture = { "", -1, "" };
	/**
	 * 
	 * [获取cpu类型和架构]
	 * 
	 * @return
	 */
	public static Object[] getCpuArchitecture() {
		if ((Integer) mArmArchitecture[1] != -1) {
			return mArmArchitecture;
		}
		try {
			InputStream is = new FileInputStream("/proc/cpuinfo");
			InputStreamReader ir = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(ir);
			try {
				String nameProcessor = "Processor";
				String nameFeatures = "Features";
				String nameModel = "model name";
				String nameCpuFamily = "cpu family";
				while (true) {
					String line = br.readLine();
					String[] pair = null;
					Log.i(TAG, line == null ? "null" : line);
					if (line == null) {
						break;
					}
					pair = line.split(":");
					if (pair.length != 2)
						continue;
					String key = pair[0].trim();
					String val = pair[1].trim();
					if (key.compareTo(nameProcessor) == 0) {
						String n = "";
						Log.i(TAG, "val:" + val);
						for (int i = val.indexOf("ARMv") + 4; i < val.length(); i++) {
							String temp = val.charAt(i) + "";
							if (temp.matches("\\d")) {
								n += temp;
							} else {
								break;
							}
						}
						Log.i(TAG, "n:" + n);
						mArmArchitecture[0] = "ARM";
						mArmArchitecture[1] = Integer.parseInt(n);
						continue;
					}

					if (key.compareToIgnoreCase(nameFeatures) == 0) {
						if (val.contains("neon")) {
							mArmArchitecture[2] = "neon";
						}
						continue;
					}

					if (key.compareToIgnoreCase(nameModel) == 0) {
						if (val.contains("Intel")) {
							mArmArchitecture[0] = "INTEL";
							mArmArchitecture[2] = "atom";
						}
						continue;
					}

					if (key.compareToIgnoreCase(nameCpuFamily) == 0) {
						mArmArchitecture[1] = Integer.parseInt(val);
						continue;
					}
				}
			} finally {
				br.close();
				ir.close();
				is.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return mArmArchitecture;
	}
	
	public static String getCpuArchString(){
		Object[] info = getCpuArchitecture();
		if(info == null || info.length != 3 || (Integer)(info[1]) == -1){
			return null;
		}
		StringBuilder sb = new StringBuilder();
		sb.append(info[0]);
		sb.append(info[1]);
		sb.append(info[2]);
		return sb.toString();
	}
}
