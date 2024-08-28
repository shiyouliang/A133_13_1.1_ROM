package com.softwinner.runin.test;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import com.softwinner.log.RuninLog;
import com.softwinner.runin.R;
import com.softwinner.runin.RuninApplication;
import com.softwinner.runin.Settings;
import com.softwinner.runin.test.base.ToggleTest;
import com.softwinner.xml.Node;

/**
 * 休眠唤醒测试
 *
 * @author zengsc
 * @version date 2013-5-17
 */
public class SleepwakeTest extends ToggleTest {
    private static final String TAG = "Runin-SleepwakeTest";
    private static final String BROADCAST_RECEIVER_ACTION = Settings.PACKAGE_NAME + ".sleepwaketest";
    private static final String WAKELOCK_TAG = Settings.PACKAGE_NAME + ".sleepwaketest";
    private PowerManager mPowerManager;
    private WakeLock mScreenonWakeLock;

    @Override
    protected void onStart(Node node) {
        Log.i(TAG, "start SleepwakeTest");
        RuninLog.append("sleepwake start open:" + mOpenDuration + "ms close:" + mCloseDuration + "ms");
        View.inflate(mContext, R.layout.sleepwake_test, mStage);
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mFirstOn = false;
        ((RuninApplication) mContext.getApplicationContext()).cpuWakeLockRelease();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "stop SleepwakeTest");
        RuninLog.append("sleepwake stop");
        if (mScreenonWakeLock != null) {
            mScreenonWakeLock.release();
            mScreenonWakeLock = null;
        }
        mPowerManager = null;
        ((RuninApplication) mContext.getApplicationContext()).cpuWakeLockAcquire();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected boolean onToggleOn() {
        Log.i(TAG, "wake up");
        RuninLog.append("sleepwake on :" + (mRepeatedTimes + 1) + "x");
        mScreenonWakeLock = mPowerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                WAKELOCK_TAG);
        mScreenonWakeLock.acquire();
        return true;
    }

    @Override
    protected boolean onToggleOff() {
        Log.i(TAG, "go to sleep");
        RuninLog.append("sleepwake off :" + (mRepeatedTimes + 1) + "x");
        if (mScreenonWakeLock != null) {
            mScreenonWakeLock.release();
            mScreenonWakeLock = null;
        }
        try {
            mPowerManager.goToSleep(SystemClock.uptimeMillis());
        } catch (Exception e) {
            RuninLog.append("sleepwake off failed at:" + (mRepeatedTimes + 1));
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected String getBroadcastReceiverAction() {
        return BROADCAST_RECEIVER_ACTION;
    }
}
