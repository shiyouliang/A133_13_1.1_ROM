package com.softwinner.runin.test.base;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.util.Log;
import android.view.ViewGroup;

import com.softwinner.runin.Settings;
import com.softwinner.runin.test.interfaces.ITest;
import com.softwinner.xml.Node;

/**
 * 用于按时间测试
 * @author zengsc
 * @version date 2013-5-15
 */
public abstract class DurationTest implements ITest {
	private static final String TAG = "Runin-DurationTest";
	private static final int ERROR_DELAYMILLIS = 5000;

	protected Context mContext;
	protected ViewGroup mStage;

	private boolean mIsRunning = false;
	private boolean mResult = false;
	private int isTestNext=0;
	protected long mDuration;

	protected StopCallback mStopCallback;
	public BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(getBroadcastReceiverAction())) {
				Log.i(TAG, getBroadcastReceiverAction() + " timeup");
				mResult = true;
				stop();
				if (mStopCallback != null)
					mStopCallback.onStop(DurationTest.this);
			}
		}
	};

	private void scheduleAlarm(long delayMillis) {
		AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(getBroadcastReceiverAction());

		PendingIntent p = PendingIntent.getBroadcast(mContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayMillis, p);
	}

	@Override
	public void create(Context context, ViewGroup stage) {
		mContext = context;
		mStage = stage;
	}

	@Override
	public final void start(Node node) {
		mContext.registerReceiver(mReceiver, new IntentFilter(getBroadcastReceiverAction()));
		mIsRunning = true;
		mResult = false;
		mDuration = node.getAttributeIntegerValue(Settings.ATTR_DURATION);
		boolean succeed = onStart(node);
		if (succeed) {
			Log.i(TAG, "start duration:" + mDuration);
			if (mDuration != 0)
				scheduleAlarm(mDuration);
		} else {
			scheduleAlarm(ERROR_DELAYMILLIS);
		}
	}

	@Override
	public final void stop() {
		mContext.unregisterReceiver(mReceiver);
		onStop();
		mIsRunning = false;
	}

	@Override
	public void destory() {
		mContext = null;
		mStage = null;
	}

	@Override
	public boolean isRunning() {
		return mIsRunning;
	}

	@Override
	public void setOnStopCallback(StopCallback callback) {
		mStopCallback = callback;
	}

	@Override
	public String getResult() {
		return mResult ? "Pass" : "Fail";
	}

	@Override
	public int getIsTestNext() {
		return isTestNext;
	}

	/**
	 * 开始时调用
	 */
	protected abstract boolean onStart(Node node);

	/**
	 * 停止时调用
	 */
	protected abstract void onStop();

	/**
	 * 获得广播注册名
	 */
	protected abstract String getBroadcastReceiverAction();
}
