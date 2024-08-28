package com.softwinner.runin.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.softwinner.log.CsvLog;
import com.softwinner.log.LogcatThread;
import com.softwinner.log.RuninLog;
import com.softwinner.runin.R;
import com.softwinner.runin.Settings;
import com.softwinner.runin.test.interfaces.ITest;
import com.softwinner.runin.test.interfaces.ITest.StopCallback;
import com.softwinner.xml.Attribute;
import com.softwinner.xml.Node;
import com.softwinner.runin.RuninApplication;

/**
 * 测试引擎
 *
 * @author zengsc
 * @version date 2013-5-16
 */
public class TestEngine implements StopCallback {
    private static final String TAG = "Runin-TestEngine";
    private static final String RUNIN_CYCLE = "cycle";
    private static final String RUNIN_STATE = "state";
    private static final String LOGCAT_PATH = "logcatpath";
    private static final String TEST_CLASS_PACKAGE = "com.softwinner.runin.test.";
    private final Context mContext;
    private final RuninApplication mApplication;
    // 屏幕锁
    private final SharedPreferences mSharedPreferences;
    private final SharedPreferences.Editor mEditor;
    private final Node mForeNode;
    private final Node mBackNode;
    // class
    private ITest[] mForegrounds;
    private ITest[] mBackgrounds;
    private final ViewGroup mStage;
    private final TextView mStateText;
    private int mCurrent;
    private int mCurrentCycle;
    private int mTotalCycle;
    private LogcatThread mLogcatThread;
    private Boolean isResetStart=true;
    private Boolean isResetStart2=false;

    public TestEngine(Context context, Node node, ViewGroup stage, TextView stateText) {
        mContext = context;
        mApplication = (RuninApplication) context.getApplicationContext();
        mSharedPreferences = context.getSharedPreferences(Settings.SHARED_NAME, Context.MODE_PRIVATE);
        mEditor = mSharedPreferences.edit();
        mCurrentCycle = getCycle();
        mCurrent = getState();
        mForeNode = node.getNode(Settings.NODE_FOREGROUND);
        mBackNode = node.getNode(Settings.NODE_BACKGROUND);
        mStage = stage;
        mStateText = stateText;
        mTotalCycle = 1;
        Attribute cycleAttr = node.getAttribute(Settings.ATTR_CYCLE);
        if (cycleAttr != null) {
            String cycleStr = cycleAttr.getValue();
            if (!TextUtils.isEmpty(cycleStr)) {
                int cycles = Integer.parseInt(cycleStr);
                if (cycles > 0)
                    mTotalCycle = cycles;
            }
        }
        // 初始化test
        int foreSize = mForeNode.getNNodes();
        mForegrounds = new ITest[foreSize];
        for (int i = 0; i < foreSize; i++) {
            String name = mForeNode.getNode(i).getName();
            try {
                mForegrounds[i] = (ITest) Class.forName(TEST_CLASS_PACKAGE + name).newInstance();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (mForegrounds[i] == null) {
                mForegrounds[i] = new BlankTest();
            }
            mForegrounds[i].create(mContext, mStage);
            mForegrounds[i].setOnStopCallback(this);
        }
        int backSize = mBackNode.getNNodes();
        mBackgrounds = new ITest[backSize];
        for (int i = 0; i < backSize; i++) {
            String name = mBackNode.getNode(i).getName();
            try {
                mBackgrounds[i] = (ITest) Class.forName(TEST_CLASS_PACKAGE + name).newInstance();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            if (mBackgrounds[i] == null) {
                mBackgrounds[i] = new BlankTest();
            }
            mBackgrounds[i].create(mContext, mStage);
        }
    }

    // 读取cycle
    public int getCycle() {
        // 启动会开始自动执行
        return mSharedPreferences.getInt(RUNIN_CYCLE, 0);
    }

    // 读取 SharedPreferences
    public int getState() {
        // 启动会开始自动执行
        return mSharedPreferences.getInt(RUNIN_STATE, 0);
    }

    // 写入 SharedPreferences
    public void saveState(int cycle, int state) {
        Log.d(TAG, "saveState:" + cycle + "," + state);
        mEditor.putInt(RUNIN_CYCLE, cycle);
        mEditor.putInt(RUNIN_STATE, state);
        mEditor.commit();
    }

    private boolean isCurrentSupport() {
        return mCurrentCycle < mTotalCycle && mCurrent >= 0 && mCurrent < mForegrounds.length;
    }

    /**
     * 是否正在跑
     */
    public boolean isRunning() {
        if (isCurrentSupport()) {
            ITest test = mForegrounds[mCurrent];
            if (test != null && test.isRunning()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 开始
     */
    public boolean start(boolean reset) {
        Log.i(TAG, "engine start:" + reset);
        setRunFlag(Settings.FLAG_RUNIN);
        isResetStart=true;
        isResetStart2=true;
        SystemProperties.set("debug.test.runin", "start");
        if (reset) {
            pause();
            mCurrentCycle = 0;
            mCurrent = 0;
            mEditor.clear();
            mEditor.commit();
        } else {
            Log.i(TAG, "current cycle:" + mCurrentCycle + ",current:" + mCurrent);
        }
        if (mCurrentCycle == 0 && mCurrent == 0) {
            boolean isRebootTest = mForegrounds[mCurrent].getClass().getSimpleName().equals("RebootTest");
            if (!isRebootTest || (isRebootTest && !mForegrounds[mCurrent].isRunning())) {
                startLogcat(true);
                RuninLog.append("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            } else {
                startLogcat(false);
            }
        } else {
            startLogcat(false);
            if (!isCurrentSupport()) {
                finish();
                return false;
            }
        }
        mApplication.setNeedClear(true);
        return excuteOne();
    }

    /**
     * 下一项测试
     */
    public void next() {
        Log.w(TAG, "excute next by user");
        if (isCurrentSupport()) {
            ITest test = mForegrounds[mCurrent];
            if (test != null && test.isRunning()) {
                test.stop();
                onStop(test);
            }
        }
    }

    /**
     * 暂停
     */
    public void pause() {
        Log.i(TAG, "engine stop");
        if (isCurrentSupport()) {
            ITest test = mForegrounds[mCurrent];
            if (test != null && test.isRunning())
                test.stop();
        }
        stopBackground();
        stopLogcat();
        SystemProperties.set("debug.test.runin", "pause");
    }

    /**
     * 停止
     */
    public void stop() {
        isResetStart=false;
        mStateText.setText(R.string.state_none);
        mStage.setVisibility(View.INVISIBLE);
        mStage.removeAllViews();
        pause();
        SystemProperties.set("debug.test.runin", "stop");
    }

    @Override
    public void onStop(ITest test) {
        Log.i(TAG, "on stop call back");
        Log.i(TAG, test.getClass().getName() + " complete!");
        SystemProperties.set("debug.test." + test.getClass().getSimpleName(), "0");
        if (isCurrentSupport() && test == mForegrounds[mCurrent]) {
            CsvLog.setResult(mCurrentCycle + 1, test.getClass().getSimpleName(), test.getResult());
            mCurrent++;
            if (mCurrent >= mForegrounds.length) {
                mCurrent = 0;
                mCurrentCycle++;
                RuninLog.append("cycle complete:" + mCurrentCycle);
                if (isCurrentSupport()) {
                    RuninLog.append("-------------------------------------" + mCurrentCycle + "/" + mTotalCycle);
                }
            }
            saveState(mCurrentCycle, mCurrent);
            boolean finish = !isCurrentSupport() || !excuteOne();
            if (finish) {
                finish();
            }
        }
    }

    private void stopBackground() {
        Log.i(TAG, "stop background");
        for (ITest back : mBackgrounds) {
            if (back != null && back.isRunning())
                back.stop();
        }
    }

    /**
     * 测试结束
     */
    private boolean isPassFlag;
    private void finish() {
        setRunFlag(Settings.FLAG_FINISH);
        stopBackground();
        Log.i(TAG, "engine finish");
        RuninLog.append(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        boolean pass = true;
        for (int i = 1; i < CsvLog.getCsvArrays().size() && pass; i++) {
            String[] arrays = CsvLog.getCsvArrays().get(i);
            String[] title = CsvLog.getCsvArrays().get(0);
            if (arrays != null && arrays.length > 0) {
                int j = 0;
                for (String result : arrays) {
                    if (TextUtils.isEmpty(result) || !(result.startsWith("Pass") || result.equals("NA"))) {
                        pass = false;
                        SystemProperties.set("debug.test." + title[j], "2");
                        break;
                    }
                    SystemProperties.set("debug.test." + title[j], "3");
                    j++;
                }
            }
        }
        SystemProperties.set("debug.test.runin", pass ? "pass" : "fail");

        View view = View.inflate(mContext, R.layout.finish_view, null);
        TextView tv = (TextView) view.findViewById(R.id.finish_text);
        tv.setTextColor(pass ? Color.GREEN : Color.RED);
        tv.setText(pass ? R.string.finish_text_pass : R.string.finish_text_fail);
        isPassFlag=pass;
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext).setTitle(R.string.finish_title).setView(view)
                .setCancelable(false).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        clearResult(mContext);
                        if(isPassFlag){
                            ((Activity) mContext).finish();
                        }

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        if (pass) {
            clearResult(mContext);
        }
    }

    /*
     * 执行
     */
    private boolean excuteOne() {
        Log.i(TAG,"yulh TestEngin mCurrent  "+mCurrent);
        int flagInt = mCurrent == 0 ? mCurrent : mCurrent - 1;
        if (mCurrent >= mForegrounds.length) {
            flagInt = mForegrounds.length - 1;
        }
        Log.i(TAG,"yulh TestEngin mForegrounds[flagInt]  "+mForegrounds[flagInt].getClass().getSimpleName());
        Log.i(TAG,"yulh TestEngin mForegrounds[flagInt].getIsTestNext()  "+mForegrounds[flagInt].getIsTestNext());
        if (isCurrentSupport() && mForegrounds[flagInt].getIsTestNext() == 1 && !isResetStart2) {
            return false;
        }
        isResetStart2=false;
        if(!isResetStart){
            return false;
        }

        int state = mCurrent;
        Log.i(TAG, "current test:" + state);
        Node fore = mForeNode.getNode(state);
        if (fore == null)
            return false;
        Log.d(TAG, "node: " + fore.toString());
        // checking background test whether can be run.
        String name = fore.getName();
        mStateText.setText(name);
        for (int i = 0; i < mBackgrounds.length; i++) {
            boolean allowRun = false;
            Node back = mBackNode.getNode(i);
            int n = back.getNNodes();
            int j = 0;
            for (; j < n; j++) {
                if (Settings.NODE_WHILE.equals(back.getNode(j).getName()) && name.equals(back.getNode(j).getValue())) {
                    allowRun = true;
                    break;
                }
            }
            if (allowRun) {
                if (!mBackgrounds[i].isRunning()) {
                    try {
                        SystemProperties.set("debug.test." + back.getNode(j).getName(), "1");
                        SystemProperties.set("debug.test.item", back.getNode(j).getName());
                        mBackgrounds[i].start(back);
                    } catch (Exception e) {
                        e.printStackTrace();
                        mBackgrounds[i] = new BlankTest();
                    }
                }
            } else {
                if (mBackgrounds[i].isRunning())
                    mBackgrounds[i].stop();
            }
        }
        // 显示stage
        mStage.setVisibility(View.VISIBLE);
        mStage.removeAllViews();
        Log.d(TAG, "start foreground");
        try {
            SystemProperties.set("debug.test." + fore.getName(), "1");
            SystemProperties.set("debug.test.item", fore.getName());
            mForegrounds[state].start(fore);
        } catch (Exception e) {
            Log.e(TAG, name + " start error.");
            e.printStackTrace();
            onStop(mForegrounds[state]);
        }
        return true;
    }

    // 设置启动flag
    private void setRunFlag(String flag) {
        File flagFile = new File(Settings.FLAG_PATH);
        BufferedWriter writer = null;
        if (flagFile.exists()) {
            try {
                writer = new BufferedWriter(new FileWriter(flagFile));
                writer.write(flag);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (writer != null)
                    try {
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
        }
        Log.d(TAG, "flag :" + flag);
    }

    private void startLogcat(boolean reset) {
        String path;
        if (reset) {
            path = Settings.LOGCAT_PATH + (new SimpleDateFormat("yyyy-MM-dd_hh_mm_ss")).format(new Date()) + ".log";
            saveLogcatPath(path);
        } else {
            path = getLogcatPath();
        }
        // 保留日志
        mLogcatThread = new LogcatThread();
        mLogcatThread.start(path);
        RuninLog.createLog(reset);
        CsvLog.loadCsv(reset);
    }

    private void stopLogcat() {
        if (mLogcatThread != null) {
            mLogcatThread.stopAndClear();
            mLogcatThread = null;
        }
    }

    // 读取logcatPath
    public String getLogcatPath() {
        return mSharedPreferences.getString(LOGCAT_PATH, null);
    }

    // 写入logcatPath
    public void saveLogcatPath(String path) {
        mEditor.putString(LOGCAT_PATH, path);
        mEditor.commit();
    }

    public static void clearResult(Context ctx) {
        SharedPreferences mPrefs = ctx.getSharedPreferences(Settings.SHARED_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor mEdtor = mPrefs.edit();
        mEdtor.clear();
        mEdtor.commit();
        CsvLog.clearCsv();
        //RuninLog.createLog(true);
        ((RuninApplication) ctx.getApplicationContext()).setNeedClear(false);
        SystemProperties.set("debug.test.runin", "clear");
    }
}
