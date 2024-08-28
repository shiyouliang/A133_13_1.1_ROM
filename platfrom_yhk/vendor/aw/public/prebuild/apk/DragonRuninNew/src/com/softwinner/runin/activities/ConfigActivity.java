package com.softwinner.runin.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.text.Editable;
import android.text.Html;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;

import com.softwinner.runin.R;
import com.softwinner.runin.RuninApplication;
import com.softwinner.runin.Settings;
import com.softwinner.runin.Utils;
import com.softwinner.runin.adapter.NodeAdapter;
import com.softwinner.runin.config.TestConfiguration;
import com.softwinner.runin.config.interfaces.IConfiguration;
import com.softwinner.xml.Attribute;
import com.softwinner.xml.Node;

import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;

/**
 * @author zengsc
 * @version date 2013-6-6
 */
public class ConfigActivity extends Activity implements MenuItem.OnMenuItemClickListener,
		AdapterView.OnItemClickListener, NodeAdapter.OnSelectionChangeListener {
	static final String TAG = "Runin-ConfigActivity";
	static final boolean DEBUG = Settings.DEBUG;
	static final int CP_START = 10;
	static final int CP_SUCCESS = 11;
	static final int CP_NOT_EXIST = 12;
	static final int CP_FAIL_EXP = 13;
	static final int CP_FAIL_MD5 = 14;

	static final int SD_REMOVE = 20;
	static final int SD_INSERT = 21;
	private RuninApplication mApplication;
	private ListView mTestsList; // 测试项列表
	private NodeAdapter mTestsAdapter;
	private ViewGroup mConfigView; // 详细配置
	private EditText mCycleEditText; // 循环圈数
	private CheckBox mTestModeCheckBox; // 测试模式
	private TestConfiguration mTestConfiguration;
    private String mLedState = "";
    private static Handler mHandler;
    private static ProgressDialog mProgressDialog;
    private static String mMountedPath;

	private MenuItem mCancelAction;
	private MenuItem mResetAction;
	private MenuItem mConfirmAction;
       
        public static final String SRC_PATH = "/sdcard/DragonFire/runin/config.xml";
        public static final String TARGET_NAME = "aging_config.xml";

        private int copyTo(String src, String dest){
            Log.d(TAG,"src="+src+", dest="+dest);
            long start = System.currentTimeMillis();
            File mSrcFile = new File(src);
            Log.d(TAG,"exists="+mSrcFile.exists()+", canRead="+mSrcFile.canRead());
            if(!mSrcFile.exists() || !mSrcFile.canRead()){
                return CP_NOT_EXIST;
            }
            File mDestFile = new File(dest);
            if(mDestFile.exists()){
                mDestFile.delete();
            }
            try {
                FileInputStream mFIS = new FileInputStream(mSrcFile);
                FileOutputStream mFOS = new FileOutputStream(mDestFile);
                byte[] buffer = new byte[16*1024];
                int rLen = 0;
                while((rLen=mFIS.read(buffer))>0){
                    mFOS.write(buffer, 0, rLen);
                }
                mFOS.flush();
                mFOS.getFD().sync();
                mFIS.close();
                mFOS.close();
                String md5Src = Utils.getMd5Hex(src);
                String md5Dst = Utils.getMd5Hex(dest);
                if(md5Src != null && md5Dst != null && md5Src.equals(md5Dst)) {
                    Log.d(TAG, "copyTo: src=" + src + ", dest=" + dest + " success, cost:" + (System.currentTimeMillis() - start) + "ms");
                    return CP_SUCCESS;
                } else {
                    return CP_FAIL_MD5;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return CP_FAIL_EXP;
            } catch (IOException e) {
                e.printStackTrace();
                return CP_FAIL_EXP;
            }
        }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String mAction = intent.getAction();
            Log.d(TAG, "mAction =" + mAction);

            if (Intent.ACTION_MEDIA_MOUNTED.equals(mAction)) {
                Toast.makeText(context, "设备插入", Toast.LENGTH_SHORT).show();
                String uriPath = intent.getData().getPath();
                mMountedPath = uriPath;
                Log.d(TAG, "intent=" + intent.toString() + ", uri_path=" + uriPath);
                mHandler.sendEmptyMessage(CP_START);
                int ret = copyTo(SRC_PATH, uriPath + "/" + TARGET_NAME);
                if (ret == CP_NOT_EXIST) {
                    Toast.makeText(context, "原配置文件不存在，请先配置并保存！！", Toast.LENGTH_LONG).show();
                } else if (ret == CP_FAIL_EXP) { // copy fail
                    mHandler.sendEmptyMessageDelayed(CP_FAIL_EXP, 1000);
                } else if (ret == CP_FAIL_MD5) { // md5sum fail
                    mHandler.sendEmptyMessageDelayed(CP_FAIL_MD5, 1000);
                } else { // success
                    mHandler.sendEmptyMessageDelayed(CP_SUCCESS, 1000);
                    //mApplication.unmountVolume(context, mMountedPath);
                }
            } else if (Intent.ACTION_MEDIA_EJECT.equals(mAction)) {
                if(mMountedPath != null && mMountedPath.equals(intent.getData().getPath())) {
                    Toast.makeText(context, "设备拔出", Toast.LENGTH_SHORT).show();
                    mHandler.sendEmptyMessageDelayed(SD_REMOVE, 1000);
                }
            }
        }
    };
      
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
		// 设置显示
		setContentView(R.layout.configuration);
		mTestsList = (ListView) findViewById(R.id.all_list);
		mCycleEditText = (EditText) findViewById(R.id.cycle_times_edittext);
		mTestModeCheckBox = (CheckBox) findViewById(R.id.test_mode_checkbox);
		mConfigView = (ViewGroup) findViewById(R.id.config_view);
		mTestsList.setOnItemClickListener(this);
		// Show view
		mTestsAdapter = new NodeAdapter(this);
		mTestsList.setAdapter(mTestsAdapter);
		mTestConfiguration = new TestConfiguration(this, mConfigView);
		// set title
		getActionBar().setTitle(
				Html.fromHtml(getString(R.string.app_config) +
						" <B><font color=\"red\">(" + getString(R.string.app_version) + ")</font></B>"));
		// 读取配置
		mTestsAdapter.setOnSelectionChangeListener(this);
		setData(mApplication.getNodeLocal());
		mTestModeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mApplication.getNodeLocal().setAttribute(Settings.ATTR_TEST_MODE, "" + isChecked);
			}
		});
		mCycleEditText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
				mApplication.getNodeLocal().setAttribute(Settings.ATTR_CYCLE, s.toString());
			}
		});
		setResult(RESULT_CANCELED);
		// 动态申请权限
		if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
				checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
				checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			String[] permissions = new String[] {android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE};
			requestPermissions(permissions, 250);
			return;
		}

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int what = msg.what;
                if (what == CP_START) {
                    mProgressDialog.setMessage("配置文件拷贝中，请勿拔出设备！");
                    mProgressDialog.show();
                    mProgressDialog.findViewById(com.android.internal.R.id.progress).setVisibility(View.VISIBLE);
                } else if (what == CP_SUCCESS) {
                    mProgressDialog.setMessage("配置文件拷贝成功，请拔出设备！");
                    mProgressDialog.show();
                    mProgressDialog.findViewById(com.android.internal.R.id.progress).setVisibility(View.GONE);
                } else if (what == CP_FAIL_EXP) {
                    mProgressDialog.setMessage("配置文件拷贝失败（EXP），请重新插拔！");
                    mProgressDialog.show();
                    mProgressDialog.findViewById(com.android.internal.R.id.progress).setVisibility(View.VISIBLE);
                } else if (what == CP_FAIL_MD5) {
                    mProgressDialog.setMessage("配置文件拷贝失败（MD5），请重新插拔！");
                    mProgressDialog.show();
                    mProgressDialog.findViewById(com.android.internal.R.id.progress).setVisibility(View.VISIBLE);
                } else if (what == SD_REMOVE) {
                    mProgressDialog.cancel();
                }
            }
        };
        mProgressDialog = new ProgressDialog(this, R.style.MyDialogTheme);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setIndeterminate(false);
        mProgressDialog.setCancelable(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter mSdFilter = new IntentFilter();
        mSdFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        mSdFilter.addAction(Intent.ACTION_MEDIA_EJECT);
        mSdFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        mSdFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        mSdFilter.addDataScheme("file");
        registerReceiver(mReceiver, mSdFilter);
        Log.d(TAG, "registerReceiver mSdFilter!");

        mLedState = SystemProperties.get("debug.test.led");
        SystemProperties.set("debug.test.led", "stop");
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
        if (mLedState.isEmpty()) {
            SystemProperties.set("debug.test.led", mLedState);
        }
    }

        @Override
        protected void onDestroy() {
            super.onDestroy();
        }

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
		for (int result : grantResults) {
			if (result != PackageManager.PERMISSION_GRANTED) {
				finish();
				return;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.configuration, menu);
		mCancelAction = menu.findItem(R.id.action_cancel);
		mResetAction = menu.findItem(R.id.action_reset);
		mConfirmAction = menu.findItem(R.id.action_confirm);
		mCancelAction.setOnMenuItemClickListener(this);
		mResetAction.setOnMenuItemClickListener(this);
		mConfirmAction.setOnMenuItemClickListener(this);
		//mCancelAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		//mResetAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		mConfirmAction.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
		return true;
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		if (item == mCancelAction) {
			setResult(RESULT_CANCELED);
			finish();
		} else if (item == mResetAction) {
			setData(mApplication.resetNode());
		} else if (item == mConfirmAction) {
			mTestsAdapter.updateNode();
			mApplication.saveNode();
			setResult(RESULT_OK);
			//finish();
			Toast toast = Toast.makeText(this,"配置文件保存成功！！", Toast.LENGTH_LONG);
			LinearLayout layout = (LinearLayout) toast.getView();
			TextView tv = (TextView) layout.getChildAt(0);
			tv.setTextColor(Color.BLACK);
			//toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
			
		}
		return false;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		mTestsAdapter.setSelection(position);
	}

	/**
	 * set node data
	 */
	public void setData(Node data) {
		if(data == null){
			return;
		}
		mTestsAdapter.setData(data);
		boolean mode = false;
		Attribute modeAttr = data.getAttribute(Settings.ATTR_TEST_MODE);
		if (modeAttr != null && !TextUtils.isEmpty(modeAttr.getValue())) {
			mode = "true".equals(modeAttr.getValue());
		}
		mTestModeCheckBox.setChecked(mode);
		// set cycle
		String cycle = null;
		Attribute cycleAttr = data.getAttribute(Settings.ATTR_CYCLE);
		if (cycleAttr != null) {
			cycle = cycleAttr.getValue();
		}
		mCycleEditText.setText(cycle);
	}

	@Override
	public void onSelection(int position) {
		if (DEBUG)
			Log.d(TAG, "on item " + position + " selection.");
		Node node = mTestsAdapter.getItem(position);
		boolean foreground = mTestsAdapter.isForeground(position);
		IConfiguration config = mTestsAdapter.getConfig(position);
		mTestConfiguration.setConfig(config, foreground, node);
	}

	@Override
	public void finish() {
		mApplication.reloadNode();
		super.finish();
	}
}
