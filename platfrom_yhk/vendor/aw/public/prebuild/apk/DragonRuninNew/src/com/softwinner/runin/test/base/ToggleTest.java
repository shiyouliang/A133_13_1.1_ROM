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
 * 用于交替测试
 * @author zengsc
 * @version date 2013-5-17
 */
public abstract class ToggleTest implements ITest {
	static final String TAG = "Runin-ToggleTest";
	static final boolean DEBUG = true;
	public static final String TOGGLE_TYPE = "type";
	public static final String TOGGLE_ON = "toggle_on";
	public static final String TOGGLE_OFF = "toggle_off";

	protected Context mContext;
	protected ViewGroup mStage;

	private boolean mIsRunning = false;
	protected boolean mFirstOn;
	protected int mRepeatCount;
	protected int mRepeatedTimes;
	protected int mOpenDuration;
	protected int mCloseDuration;
	private int isTestNext=0;

	private StopCallback mStopCallback;
	public BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(getBroadcastReceiverAction())) {
				String type = intent.getStringExtra(TOGGLE_TYPE);
				if (type.equals(TOGGLE_ON)) {
					toggleOn();
				} else if (type.equals(TOGGLE_OFF)) {
					toggleOff();
				}
			}
		}
	};

	private void scheduleAlarm(long delayMillis, String toggleType) {
		AlarmManager am = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
		Intent i = new Intent(getBroadcastReceiverAction());
		i.putExtra(TOGGLE_TYPE, toggleType);
		PendingIntent p = PendingIntent.getBroadcast(mContext, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
		am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + delayMillis, p);
	}

	private void sendBroadcast(String toggleType) {
		Intent i = new Intent(getBroadcastReceiverAction());
		i.putExtra(TOGGLE_TYPE, toggleType);
		mContext.sendBroadcast(i);
	}

	/**
	 * 打开
	 */
	private void toggleOn() {
		Log.i(TAG, "onToggleOn");
		if (!onToggleOn()) {
			Log.e(TAG, "failed and stop onToggleOn." + getClass().getSimpleName());
			Log.e(TAG, "Toggle repeat:" + mRepeatedTimes);
			stop();
			if (mStopCallback != null)
				mStopCallback.onStop(this);
			return;
		}
		if (!mFirstOn) {
			repeat();
		}
		if (DEBUG)
			Log.d(TAG, "open duration:" + mOpenDuration);
		if (mOpenDuration > 0)
			scheduleAlarm(mOpenDuration, TOGGLE_OFF);
		else
			sendBroadcast(TOGGLE_OFF);
	}

	/**
	 * 关闭
	 */
	private void toggleOff() {
		Log.i(TAG, "toggleOff");
		if (!onToggleOff()) {
			Log.e(TAG, "failed and stop onToggleOff." + getClass().getSimpleName());
			Log.e(TAG, "Toggle repeat:" + mRepeatedTimes);
			stop();
			if (mStopCallback != null)
				mStopCallback.onStop(this);
			return;
		}
		if (mFirstOn) {
			repeat();
		}
		if (DEBUG)
			Log.d(TAG, "close duration:" + mCloseDuration);
		if (mCloseDuration > 0)
			scheduleAlarm(mCloseDuration, TOGGLE_ON);
		else
			sendBroadcast(TOGGLE_ON);
	}

	/**
	 * 重复加一次
	 */
	private void repeat() {
		mRepeatedTimes++;
		Log.i(TAG, "RepeatedTimes:" + mRepeatedTimes);
		if (mRepeatCount > 0 && mRepeatedTimes >= mRepeatCount) {
			stop();
			if (mStopCallback != null)
				mStopCallback.onStop(this);
			return;
		}
	}

	@Override
	public void create(Context context, ViewGroup stage) {
		mContext = context;
		mStage = stage;
	}

	@Override
	public void start(Node node) {
		Log.i(TAG, "register receiver" + getBroadcastReceiverAction());
		mContext.registerReceiver(mReceiver, new IntentFilter(getBroadcastReceiverAction()));
		Log.d(TAG, "is running " + mIsRunning);
		mIsRunning = true;
		mFirstOn = false;
		mRepeatCount = node.getAttributeIntegerValue(Settings.ATTR_REPEAT_COUNT);
		mRepeatedTimes = 0;
		mOpenDuration = node.getAttributeIntegerValue(Settings.ATTR_OPEN_DURATION);
		mCloseDuration = node.getAttributeIntegerValue(Settings.ATTR_CLOSE_DURATION);
		onStart(node);
		Log.i(TAG, "start RepeatCount:" + mRepeatCount);
		if (mRepeatCount > 0 && mRepeatedTimes >= mRepeatCount) {
			stop();
			if (mStopCallback != null)
				mStopCallback.onStop(this);
			return;
		}
		if (mFirstOn)
			toggleOn();
		else
			toggleOff();
	}

	@Override
	public void stop() {
		Log.i(TAG, "unregister receiver receiver");
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
		return mRepeatedTimes >= mRepeatCount ? "Pass " + mRepeatCount + " Loop" : "Fail " + (mRepeatedTimes + 1)
				+ " Loop";
	}

	@Override
	public int getIsTestNext() {
		return isTestNext;
	}

	/**
	 * 开始时调用
	 */
	protected abstract void onStart(Node node);

	/**
	 * 停止时调用
	 */
	protected abstract void onStop();

	/**
	 * 打开时调用
	 */
	protected abstract boolean onToggleOn();

	/**
	 * 关闭时调用
	 */
	protected abstract boolean onToggleOff();

	/**
	 * 获得广播注册名
	 */
	protected abstract String getBroadcastReceiverAction();
}
