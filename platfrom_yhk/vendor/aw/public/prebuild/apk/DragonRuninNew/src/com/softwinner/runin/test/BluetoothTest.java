package com.softwinner.runin.test;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import com.softwinner.log.RuninLog;
import com.softwinner.runin.Settings;
import com.softwinner.runin.test.base.ToggleTest;
import com.softwinner.xml.Node;

/**
 * 蓝牙开关测试
 * @author zengsc
 * @version date 2013-5-17
 */
public class BluetoothTest extends ToggleTest {
	private static final String TAG = "Runin-BluetoothTest";
	private static final String BROADCAST_RECEIVER_ACTION = Settings.PACKAGE_NAME + ".bluetoothtest";
	private BluetoothAdapter mBluetoothAdapter;

	@Override
	protected void onStart(Node node) {
		Log.i(TAG, "start BluetoothTest");
		RuninLog.append("bluetooth start open:" + mOpenDuration + "ms close:" + mCloseDuration + "ms");
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	@Override
	protected void onStop() {
		Log.i(TAG, "stop BluetoothTest");
		RuninLog.append("bluetooth stop");
		mBluetoothAdapter = null;
	}

	@Override
	protected boolean onToggleOn() {
		Log.i(TAG, "turn bluetooth on");
		RuninLog.append("bluetooth on :" + (mRepeatedTimes + 1) + "x");
		if (mBluetoothAdapter != null) {
			if (!mBluetoothAdapter.isEnabled())
				mBluetoothAdapter.enable();
			return true;
		}
		return false;
	}

	@Override
	protected boolean onToggleOff() {
		Log.i(TAG, "turn bluetooth off");
		RuninLog.append("bluetooth off :" + (mRepeatedTimes + 1) + "x");
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isEnabled())
				mBluetoothAdapter.disable();
			return true;
		}
		return false;
	}

	@Override
	protected String getBroadcastReceiverAction() {
		return BROADCAST_RECEIVER_ACTION;
	}
}
