package com.softwinner.runin.test;

import android.hardware.Camera;
import android.util.Log;

import com.softwinner.log.RuninLog;
import com.softwinner.runin.Settings;
import com.softwinner.runin.test.base.ToggleTest;
import com.softwinner.xml.Node;

/**
 * Camera开关交替测试
 * @author zengsc
 * @version date 2013-5-17
 */
public class CameraTest extends ToggleTest {
	private static final String TAG = "Runin-CameraTest";
	private static final String BROADCAST_RECEIVER_ACTION = Settings.PACKAGE_NAME + ".cameratest";
	private int mCameraNumber;
	private Camera mCurrentCamera;

	@Override
	protected void onStart(Node node) {
		Log.i(TAG, "start CameraTest");
		RuninLog.append("camera start open:" + mOpenDuration + "ms close:" + mCloseDuration + "ms");
		mCameraNumber = Camera.getNumberOfCameras();
		Log.i(TAG, "camera number " + mCameraNumber);
	}

	@Override
	protected void onStop() {
		if (mCurrentCamera != null) {
			mCurrentCamera.release();
			mCurrentCamera = null;
		}
		Log.i(TAG, "stop CameraTest");
		RuninLog.append("camera stop");
	}

	@Override
	protected boolean onToggleOn() {
		Log.i(TAG, "turn camera on");
		RuninLog.append("camera on :" + (mRepeatedTimes + 1) + "x");
		if (mCameraNumber > 0) {
			try {
				mCurrentCamera = Camera.open(1);
				Log.i(TAG, mCurrentCamera.toString());
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return false;
	}

	@Override
	protected boolean onToggleOff() {
		Log.i(TAG, "turn camera off");
		RuninLog.append("camera off :" + (mRepeatedTimes + 1) + "x");
		if (mCurrentCamera != null) {
			mCurrentCamera.release();
			mCurrentCamera = null;
		}
		return true;
	}

	@Override
	protected String getBroadcastReceiverAction() {
		return BROADCAST_RECEIVER_ACTION;
	}
}
