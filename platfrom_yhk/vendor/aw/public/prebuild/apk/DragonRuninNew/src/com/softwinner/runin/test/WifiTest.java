package com.softwinner.runin.test;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.softwinner.log.RuninLog;
import com.softwinner.runin.Settings;
import com.softwinner.runin.test.base.ToggleTest;
import com.softwinner.xml.Node;

/**
 * WiFi开关交替测试
 * @author zengsc
 * @version date 2013-5-17
 */
public class WifiTest extends ToggleTest {
	private static final String TAG = "Runin-WifiTest";
	private static final String BROADCAST_RECEIVER_ACTION = Settings.PACKAGE_NAME + ".wifitest";
	private WifiManager mWifiManager;

	@Override
	protected void onStart(Node node) {
		Log.i(TAG, "start WifiTest");
		RuninLog.append("wifi start open:" + mOpenDuration + "ms close:" + mCloseDuration + "ms");
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
	}

	@Override
	protected void onStop() {
		mWifiManager = null;
		Log.i(TAG, "stop WifiTest");
		RuninLog.append("wifi stop");
	}

	@Override
	protected boolean onToggleOn() {
		Log.i(TAG, "turn wifi on");
		RuninLog.append("wifi on :" + (mRepeatedTimes + 1) + "x");
		return mWifiManager.setWifiEnabled(true);
	}

	@Override
	protected boolean onToggleOff() {
		Log.i(TAG, "turn wifi off");
		RuninLog.append("wifi off :" + (mRepeatedTimes + 1) + "x");
		return mWifiManager.setWifiEnabled(false);
	}

	@Override
	protected String getBroadcastReceiverAction() {
		return BROADCAST_RECEIVER_ACTION;
	}


}
