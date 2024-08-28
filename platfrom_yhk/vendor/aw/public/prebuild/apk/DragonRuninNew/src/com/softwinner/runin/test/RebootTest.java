package com.softwinner.runin.test;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.softwinner.log.RuninLog;
import com.softwinner.runin.R;
import com.softwinner.runin.Settings;
import com.softwinner.runin.test.interfaces.ITest;
import com.softwinner.xml.Node;

/**
 * 重启测试
 *
 * @author zengsc
 * @version date 2013-5-19
 */
public class RebootTest implements ITest, Runnable {
    private static final String TAG = "Runin-RebootTest";
    // shared preferences
    private static final String REBOOTING = "rebooting";
    private static final String REPEATED_TIMES = "repeatedtimes";

    protected Context mContext;
    protected ViewGroup mStage;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor mEditor;
    private int mRepeatCount;
    private int mCurrentTimes;
    private String mReason;

    private StopCallback mStopCallback;
    private Handler mRebootHandler = new Handler(); // 重启延迟
    private final int delay = 3000;

    @Override
    public void create(Context context, ViewGroup stage) {
        mContext = context;
        mStage = stage;
        mSharedPreferences = context.getSharedPreferences(Settings.SHARED_NAME, Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
    }

    @Override
    public void start(Node node) {
        Log.i(TAG, "RebootTest start");
        setRebooting(true);

        // add by yulh
        mEditor.putBoolean("is_aleady_open_hand", true);
        mEditor.commit();
        // add by yulh

        View.inflate(mContext, R.layout.reboot_test, mStage);
        mRepeatCount = node.getAttributeIntegerValue(Settings.ATTR_REPEAT_COUNT);
        mReason = null;
        Node reasonNode = node.getNode(Settings.NODE_REASON);
        if (reasonNode != null) {
            mReason = reasonNode.getValue();
        }
        mCurrentTimes = getRepeatedTimes();
        Log.i(TAG, "Count:" + mCurrentTimes + "/" + mRepeatCount);
        if (mCurrentTimes == 0) {
            RuninLog.append("reboot start repeat:" + mRepeatCount);
        } else if (mCurrentTimes <= mRepeatCount) {
            RuninLog.append("reboot pass: " + mCurrentTimes + "x");
        }
        if (mCurrentTimes < mRepeatCount) {
            mCurrentTimes++;
            saveRepeatedTimes(mCurrentTimes);
            Toast.makeText(mContext, (delay / 1000) + "秒后重启！", Toast.LENGTH_SHORT).show();
            mRebootHandler.postDelayed(this, delay);
        } else {
            // add by yulh
            mEditor.putBoolean("is_aleady_open_hand", false);
            mEditor.commit();
            // add by yulh

            saveRepeatedTimes(0);
            RuninLog.append("reboot stop");
            if (mStopCallback != null)
                mStopCallback.onStop(this);
        }
    }

    @Override
    public void stop() {
        Log.e(TAG, "stop reboot");
        mRebootHandler.removeCallbacks(this);
        RuninLog.append("reboot stop");
        setRebooting(false);
    }

    @Override
    public void destory() {
        mContext = null;
        mStage = null;
        mSharedPreferences = null;
        mEditor = null;
    }

    @Override
    public boolean isRunning() {
        return getRebooting();
    }

    @Override
    public void setOnStopCallback(StopCallback callback) {
        mStopCallback = callback;
    }

    @Override
    public void run() {
        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        try {
            Log.i(TAG, "Reboot reason:" + mReason);
            pm.reboot(mReason);
        } catch (Exception e) {

            // add by yulh
            mEditor.putBoolean("is_aleady_open_hand", false);
            mEditor.commit();
            // add by yulh

            RuninLog.append("reboot failed at:" + getRepeatedTimes());
            e.printStackTrace();
            saveRepeatedTimes(0);
            if (mStopCallback != null)
                mStopCallback.onStop(this);
        }
    }

    // 是否重启测试中
    protected boolean getRebooting() {
        return mSharedPreferences.getBoolean(REBOOTING, false);
    }

    // 设置是否重启测试中
    protected void setRebooting(boolean reboot) {
        mEditor.putBoolean(REBOOTING, reboot);
        mEditor.commit();
    }

    // 读取当前次数
    protected int getRepeatedTimes() {
        return mSharedPreferences.getInt(REPEATED_TIMES, 0);
    }

    // 写入 当前次数
    protected void saveRepeatedTimes(int times) {
        mEditor.putInt(REPEATED_TIMES, times);
        mEditor.commit();
    }

    @Override
    public String getResult() {
        return mCurrentTimes >= mRepeatCount ? "Pass " + mRepeatCount + " Loop" : "Fail " + mCurrentTimes + " Loop";
    }

    @Override
    public int getIsTestNext() {
        return 0;
    }
}
