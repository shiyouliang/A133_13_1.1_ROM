package com.softwinner.log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.util.Log;

import com.softwinner.runin.Settings;

/**
 * 记录测试结果
 * @author zengsc
 * @version date 2013-5-24
 */
public class RuninLog {
	private static final String TAG = "Runin-RuninLog";
	private static final String LOG_FILE_PATH = Settings.LOGCAT_PATH + File.separator + "runinlog.txt";
	private static File mLogFile;
	private static SimpleDateFormat mDateFormat;

	/**
	 * 初始化创建log file
	 */
	public static File createLog(boolean reset) {
		if (mLogFile == null) {
			mLogFile = new File(LOG_FILE_PATH);
			mDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
			if (mLogFile.exists() && reset)
				mLogFile.delete();
			if (!mLogFile.exists()) {
				if (!mLogFile.getParentFile().exists())
					mLogFile.getParentFile().mkdirs();
				try {
					mLogFile.createNewFile();
					Log.i(TAG, "createLog: create log file: " + mLogFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return mLogFile;
	}

	/**
	 * 添加日志
	 */
	public static synchronized void append(String content) {
		Log.i(TAG, "append: " + content);
		if (mLogFile == null)
			return;
		try {
			String time = mDateFormat.format(new Date());
			StringBuffer buffer = new StringBuffer();
			buffer.append(time);
			buffer.append(" ");
			buffer.append(content);
			buffer.append("\n");
			// 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
			FileWriter writer = new FileWriter(mLogFile, true);
			writer.write(buffer.toString());
			writer.flush();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
