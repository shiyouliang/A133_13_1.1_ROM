package com.softwinner.log;

import java.io.File;
import java.io.IOException;

import com.softwinner.runin.Settings;

/**
 * Logcat线程
 * @author zengsc
 * @version date 2013-5-19
 */
public class LogcatThread extends Thread {
	private String mLogcatPath = Settings.LOGCAT_PATH + "default";

	@Override
	public void run() {
		super.run();
		File file = new File(Settings.LOGCAT_PATH);
		if (!file.exists())
			file.mkdirs();
		try {
			// Runtime.getRuntime().exec("logcat -c");
			Runtime.getRuntime().exec("logcat -f " + mLogcatPath + " *:V");
		} catch (IOException e) {
			e.printStackTrace();
		}
	};

	/**
	 * logcat文件路径
	 */
	public void start(String path) {
		if (path != null)
			mLogcatPath = path;
		start();
	}

	/**
	 * 挂起进程并清空logcat
	 */
	public void stopAndClear() {
		interrupt();
		try {
			Runtime.getRuntime().exec("logcat -c");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
