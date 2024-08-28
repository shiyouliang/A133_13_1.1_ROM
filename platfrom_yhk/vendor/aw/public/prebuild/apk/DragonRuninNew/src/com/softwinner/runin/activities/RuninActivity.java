package com.softwinner.runin.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.Html;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.softwinner.log.RuninLog;
import com.softwinner.runin.R;
import com.softwinner.runin.RuninApplication;
import com.softwinner.runin.Settings;
import com.softwinner.runin.Utils;
import com.softwinner.runin.test.TestEngine;
import com.softwinner.xml.Attribute;
import com.softwinner.xml.Node;

import android.app.KeyguardManager;
import android.content.Context;
import android.widget.Toast;

/**
 * 循环测试Activity
 *
 * @author zengsc
 * @version date 2013-5-15
 */
public class RuninActivity extends Activity implements MenuItem.OnMenuItemClickListener {
    public static final String TAG = "Runin-RuninActivity";
    private static final int GET_SETTINGS = 1;
    private RuninApplication mApplication;
    private Node mNode;
    private TestEngine mEngine;
    private boolean mTestMode;

    private ViewGroup mStage;
    private TextView mStateText;
    private MenuItem mSettingsAction; // 重置设置
    private MenuItem mStartAction;
    private MenuItem mNextAction;
    private MenuItem mStopAction;
    private MenuItem mExitAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApplication = (RuninApplication) getApplication();
        // 设置ACTIONBAR
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        // 设置全屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        // WindowManager.LayoutParams params = getWindow().getAttributes();
        // params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        // getWindow().setAttributes(params);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Settings.density = dm.density;
        Settings.densityDpi = dm.densityDpi;
        Settings.screenWidth = dm.widthPixels;
        Settings.screenHeight = dm.heightPixels;
        // 设置显示
        setContentView(R.layout.runin);
        mStage = (ViewGroup) findViewById(R.id.test_parent);
        mStateText = (TextView) findViewById(R.id.state_text);
        // set title
        getActionBar().setTitle(
                Html.fromHtml(getString(R.string.app_name) +
                        " <B><font color=\"red\">(" + getString(R.string.app_version) + ")</font></B>"));

        boolean clear_result = getIntent().getBooleanExtra("clear_result", false);
        if (clear_result) {
            Log.d(TAG, "clear_result is true.");
            TestEngine.clearResult(this);
            finish();
            return;
        }

        mApplication.cpuWakeLockAcquire();
        mApplication.screenWakeLockAcquire();

        KeyguardManager mKeyguard = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        mKeyguard.requestDismissKeyguard(this, null);

        // 动态申请权限
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE};
            requestPermissions(permissions, 250);
            return;
        }
        load();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                finish();
                return;
            }
        }
        load();
    }

    private void load() {
        // 读取配置
        Log.d(TAG, "load: mApplication.mAgingCfg = " + mApplication.mAgingCfg);
        if (mApplication.mAgingCfg == null) {
            String agingCfg = Utils.getAgingConfig(this);
            Log.d(TAG, "agingCfg = " + agingCfg);
            if (agingCfg == null) {
                //Toast.makeText(this,"未找到配置文件！！", Toast.LENGTH_LONG).show();
                RuninLog.append("config file not exist!");
                //return;
            }
            mApplication.mAgingCfg = agingCfg;
        }

        mNode = mApplication.getNode();

        if (mNode == null) {
            mNode = mApplication.getNodeLocal();
        }

        Log.d(TAG, " mNode:\n" + mNode);

        if (mApplication.isAgCfgFromSdcard()) {
            SystemProperties.set("debug.test.agcfg.from.sdcard", "1");
            SystemProperties.set("debug.test.sdcard.exist", "1");
            mApplication.unmountVolume(this, mApplication.mAgingCfg);
        }

        if (mNode == null) {
            SystemProperties.set("debug.test.runin", "config_error");
            Toast.makeText(this, "配置文件损坏！！", Toast.LENGTH_LONG).show();
            RuninLog.append("config file damaged!");
            return;
        }
        mTestMode = false;
        Attribute modeAttr = mNode.getAttribute(Settings.ATTR_TEST_MODE);
        if (modeAttr != null && !TextUtils.isEmpty(modeAttr.getValue())) {
            mTestMode = "true".equals(modeAttr.getValue());
        }

        // 启动引擎
        mEngine = new TestEngine(this, mNode, mStage, mStateText);
        boolean restart = getIntent().getBooleanExtra("restart", false);
        boolean start = mEngine.start(restart);
        updateUi(start);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (mEngine != null) {
            boolean restart = intent.getBooleanExtra("restart", false);
            boolean start = mEngine.start(restart);
            updateUi(start);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.runin, menu);
        mSettingsAction = menu.findItem(R.id.action_settings);
        mStartAction = menu.findItem(R.id.action_start);
        mNextAction = menu.findItem(R.id.action_next);
        mStopAction = menu.findItem(R.id.action_stop);
        mExitAction = menu.findItem(R.id.action_exit);
        mSettingsAction.setOnMenuItemClickListener(this);
        mStartAction.setOnMenuItemClickListener(this);
        mNextAction.setOnMenuItemClickListener(this);
        mStopAction.setOnMenuItemClickListener(this);
        mExitAction.setOnMenuItemClickListener(this);
        mSettingsAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mStartAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mNextAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mStopAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        mExitAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        updateUi(mEngine != null && mEngine.isRunning());
        return true;
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        if (mEngine != null) {
            String item = SystemProperties.get("debug.test.item");
            Log.d(TAG, "onStart: item = " + item + ", mEngine.isRunning() = " + mEngine.isRunning());
            if (item != null && (item.equals("VideoTest") || item.equals("ThreeDimensionalTest") || item.equals("TwoDimensionalTest"))) {
                if (!mEngine.isRunning()) {
                    boolean restart = getIntent().getBooleanExtra("restart", false);
                    boolean start = mEngine.start(restart);
                    updateUi(start);
                }
            }
        }
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        if (mEngine != null) {
            String item = SystemProperties.get("debug.test.item");
            Log.d(TAG, "onStop: item = " + item + ", mEngine.isRunning() = " + mEngine.isRunning());
            if (item != null && (item.equals("VideoTest") || item.equals("ThreeDimensionalTest") || item.equals("TwoDimensionalTest"))) {
                if (mEngine.isRunning()) {
                    mEngine.pause();
                }
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mApplication.cpuWakeLockRelease();
        if (mEngine != null)
            mEngine.pause();
        mEngine = null;
        super.onDestroy();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (mEngine == null) {
            Toast.makeText(this, "测试引擎未启动！！", Toast.LENGTH_LONG).show();
            return false;
        }
        if (item == mSettingsAction) {
            mEngine.pause();
            Intent intent = new Intent(this, ConfigActivity.class);
            startActivityForResult(intent, GET_SETTINGS);
        } else if (item == mStartAction) {
            mEngine.start(true);
            updateUi(true);
        } else if (item == mNextAction) {
            mEngine.next();
        } else if (item == mStopAction) {
            mEngine.stop();
            updateUi(false);
        } else if (item == mExitAction) {
            mEngine.pause();
            finish();
        }
        return false;
    }

    /**
     * 更新按钮状态
     */
    public void updateUi(boolean isRunning) {
        if (mSettingsAction != null) {
            mSettingsAction.setEnabled(!isRunning && mTestMode);
            mSettingsAction.setVisible(mTestMode);
        }
        if (mNextAction != null) {
            mNextAction.setEnabled(isRunning && mTestMode);
            mNextAction.setVisible(mTestMode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_SETTINGS && resultCode == RESULT_OK) {
            mNode = mApplication.getNode();
            mEngine = new TestEngine(this, mNode, mStage, mStateText);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
