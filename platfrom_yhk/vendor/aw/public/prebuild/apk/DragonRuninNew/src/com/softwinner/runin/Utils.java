package com.softwinner.runin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.List;

import android.content.Context;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;

import com.softwinner.xml.Node;
import com.softwinner.xml.Parser;
import com.softwinner.xml.ParserException;
import com.softwinner.xml.parse.XmlPullParser;
import com.softwinner.runin.RuninApplication;

/**
 * utils 4 settings
 *
 * @author zengsc
 * @version date 2013-5-16
 */
public class Utils {
	private static final String TAG = "Runin-Utils";

	public static String getAgingConfig(Context context) {
		String targetFile = "aging_config.xml";
		RuninApplication app = (RuninApplication) context.getApplicationContext();
		try {
			StorageManager storageManager = (StorageManager) context.getSystemService(StorageManager.class);
			List<VolumeInfo> volumes = storageManager.getVolumes();
			for (VolumeInfo vol : volumes) {
				if (VolumeInfo.TYPE_PUBLIC == vol.getType()) {
					String path = vol.getPath() != null ? vol.getPath().toString() : null;
					if(path == null) {
						path = vol.getInternalPath() != null ? vol.getInternalPath().toString() : null;
						if (path == null) {
							Log.d(TAG, "find public volume, but path null, vol = " + vol);
							continue;
						}
					}
					Log.d(TAG, "find public volume: " + path + ", description = " + storageManager.getBestVolumeDescription(vol));
					File rootDir = new File(path);
					File[] mTargetFiles = rootDir.listFiles();
					if (mTargetFiles == null) {
						Log.d(TAG, "  scan files: nothing under " + path);
						continue;
					}
					for (int j = 0; j < mTargetFiles.length; ++j) {
						Log.d(TAG, "  scan files: " + mTargetFiles[j].getName());
						if (mTargetFiles[j].getName().equals(targetFile)) {
							if(vol.getDisk() != null && vol.getDisk().isSd()) {
								app.setAgCfgFromSdcard(true);
								Log.d(TAG, "The aging config file is on sdcard");
							}
							return mTargetFiles[j].getAbsolutePath();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

	/**
	 * 获得设置
	 */
	public static Node getSettings() {
		InputStream is = getSettingsIS(Settings.CONFIG_FILE);
		Node settings = parseNode(is);
		return settings;
	}

	/**
	 * 获得默认设置
	 */
	public static Node getDefaultSettings(Context context) {
		InputStream is = null;
		try {
			is = context.getResources().getAssets().open(Settings.DEFAULT_CONFIG_FILE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Node settings = parseNode(is);
		return settings;
	}

	public static Node getSettings(String cfgPath) {
		InputStream is = getSettingsIS(cfgPath);
		Node settings = parseNode(is);
		return settings;
	}

	private static InputStream getSettingsIS(String path) {
		InputStream is = null;
		if(path==null){
			return null;
		}
		try {
			File file = new File(path);
			if (!file.exists() || file.length() == 0)
				return null;
			FileInputStream fis = new FileInputStream(file);
			byte buffer[] = new byte[(int) file.length()];
			fis.read(buffer);
			fis.close();
			is = new ByteArrayInputStream(buffer);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return is;
	}

	private static Node parseNode(InputStream is) {
		if (is != null) {
			Parser parser = new XmlPullParser();
			try {
				return parser.parse(is);
			} catch (ParserException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 保存设置
	 */
	public static void saveSetting(Node node) {
		File file = new File(Settings.CONFIG_FILE);
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		byte content[] = node.toString().getBytes();
		ByteArrayInputStream bais = new ByteArrayInputStream(content);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			byte writeBuf[] = new byte[1024 * 512];
			int length = 0;
			while ((length = bais.read(writeBuf)) > 0) {
				fos.write(writeBuf, 0, length);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				bais.close();
				if (fos != null) {
					fos.flush();
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 重置默认设置
	 */
	public static Node resetSettings(Context context) {
		Node node = getDefaultSettings(context);
		saveSetting(node);
		return node;
	}

	public static String getMd5Hex(String path) {
		File file = new File(path);

		if (!file.isFile()) {
			return null;
		}
		MessageDigest digest = null;
		FileInputStream in = null;
		byte buffer[] = new byte[1024];
		int len;
		try {
			digest = MessageDigest.getInstance("MD5");
			in = new FileInputStream(file);
			while ((len = in.read(buffer, 0, 1024)) != -1) {
				digest.update(buffer, 0, len);
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return bytesToHexString(digest.digest());
	}

	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}
}
