package com.softwinner.runin.test;

import android.content.Context;
import android.util.Log;
import android.view.ViewGroup;

import com.softwinner.runin.test.interfaces.ITest;
import com.softwinner.xml.Node;

/**
 * 空白测试项
 * @author zengsc
 * @version date 2013-5-21
 */
public class BlankTest implements ITest {
	private static final String TAG = "Runin-BlankTest";
	private StopCallback mStopCallback;

	@Override
	public void create(Context context, ViewGroup stage) {

	}

	@Override
	public void start(Node node) {
		Log.w(TAG, "BlankTest start");
		if (mStopCallback != null)
			mStopCallback.onStop(this);
	}

	@Override
	public void stop() {
		Log.w(TAG, "BlankTest stop");
	}

	@Override
	public void destory() {

	}

	@Override
	public boolean isRunning() {
		return false;
	}

	@Override
	public void setOnStopCallback(StopCallback callback) {
		mStopCallback = callback;
	}

	@Override
	public String getResult() {
		return "Pass Blank";
	}

	@Override
	public int getIsTestNext() {
		return 0;
	}
}
