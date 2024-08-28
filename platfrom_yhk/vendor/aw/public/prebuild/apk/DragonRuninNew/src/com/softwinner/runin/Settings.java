package com.softwinner.runin;

import java.io.File;

import android.os.Environment;

import com.softwinner.runin.config.BluetoothTestConfig;
import com.softwinner.runin.config.CameraTestConfig;
import com.softwinner.runin.config.MemtesterTestConfig;
import com.softwinner.runin.config.RebootTestConfig;
import com.softwinner.runin.config.SleepwakeTestConfig;
import com.softwinner.runin.config.ThreeDimensionalTestConfig;
import com.softwinner.runin.config.TwoDimensionalTestConfig;
import com.softwinner.runin.config.VideoTestConfig;
import com.softwinner.runin.config.WifiTestConfig;
import com.softwinner.runin.config.interfaces.IConfiguration;

/**
 * Settings
 * @author zengsc
 * @version date 2013-5-16
 */
public class Settings {
	public static final boolean DEBUG = true;
	public static final String PACKAGE_NAME = "com.softwinner.runin";
	// autorun flag
	public static final String FLAG_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator
			+ "boot_flag";
	public static final String FLAG_RUNIN = "RUNIN";
	public static final String FLAG_FINISH = "FINISH";
	// configuration const
	public static final String RUNIN_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
			+ File.separator + "DragonFire" + File.separator + "runin" + File.separator;
	public static final String CRASH_PATH = RUNIN_PATH + "crash" + File.separator;
	public static final String LOGCAT_PATH = RUNIN_PATH + "logcat" + File.separator;
	public static final String CONFIG_FILE = RUNIN_PATH + "config.xml";
	public static final String DEFAULT_CONFIG_FILE = "default_config.xml";
	public static final String NODE_FOREGROUND = "Foreground";
	public static final String NODE_BACKGROUND = "Background";
	public static final String NODE_WHILE = "while"; // source path
	public static final String NODE_SOURCE = "src"; // source path
	public static final String NODE_REASON = "reason"; // reboot reason
	public static final String ATTR_TEST_MODE = "test"; // test mode
	public static final String ATTR_CYCLE = "cycle"; // foreground repeat time
	public static final String ATTR_DURATION = "duration"; // duration
	public static final String ATTR_DELAY = "delay"; // 2D change delay
	public static final String ATTR_REPEAT_COUNT = "repeat"; // toggle repeat time
	public static final String ATTR_OPEN_DURATION = "open"; // toggle on duration
	public static final String ATTR_CLOSE_DURATION = "close"; // toggle on duration
	public static final String ATTR_MEMSIZE = "memsize"; // memsize

	private static IConfiguration[] FOREGROUND_CONFIGURATIONS;
	private static IConfiguration[] BACKGROUND_CONFIGURATIONS;

	public static final IConfiguration[] getForegroundConfig() {
		if (FOREGROUND_CONFIGURATIONS == null)
			FOREGROUND_CONFIGURATIONS = new IConfiguration[] { new VideoTestConfig(), new ThreeDimensionalTestConfig(),
					new TwoDimensionalTestConfig(), new MemtesterTestConfig(), new SleepwakeTestConfig(),
					new RebootTestConfig() };
		return FOREGROUND_CONFIGURATIONS;
	}

	public static final IConfiguration[] getBackgroundConfig() {
		if (BACKGROUND_CONFIGURATIONS == null)
			BACKGROUND_CONFIGURATIONS = new IConfiguration[] { new WifiTestConfig(), new BluetoothTestConfig(),
					new CameraTestConfig() };
		return BACKGROUND_CONFIGURATIONS;
	}

	// shared preferences
	public static final String SHARED_NAME = "RUNIN";
	// global value
	public static float density; // 屏幕密度（像素比例：0.75/1.0/1.5/2.0）
	public static int densityDpi; // 屏幕密度（每寸像素：120/160/240/320）
	public static int screenWidth; // 屏幕宽（像素，如：480px）
	public static int screenHeight; // 屏幕高（像素，如：800px）
}
