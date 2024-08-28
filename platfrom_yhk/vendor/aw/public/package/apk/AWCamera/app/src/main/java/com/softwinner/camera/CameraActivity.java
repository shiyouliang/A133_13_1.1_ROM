package com.softwinner.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.softwinner.camera.cameracontrol.CameraManager;
import com.softwinner.camera.data.CameraData;
import com.softwinner.camera.data.Contants;
import com.softwinner.camera.receiver.PowerReceiver;
import com.softwinner.camera.ui.UIManager;
import com.softwinner.camera.utils.CameraUtils;
import com.softwinner.camera.utils.SharedPreferencesUtils;
import com.softwinner.camera.views.PaperFragment;

import java.lang.ref.WeakReference;

import static android.view.KeyEvent.KEYCODE_BACK;


public class CameraActivity extends Activity implements PowerReceiver.Callback,PermissionLayoutManager.PermissionLayoutManagerListener {//add by heml,for kill awcamera

    private PaperFragment mViewPagerFragment;
    private static final String TAG = "CameraActivity";
    public static final String SECURE_CAMERA_EXTRA = "secure_camera";
    private UIManager mUiManager;
    private OrientationEventListener mOrientationEventListener;
    private static final int MSG_CLEAR_SCREEN_ON_FLAG = 2;
    private static final long SCREEN_DELAY_MS = 2 * 60 * 1000; // 2 mins.
    private int mOrientationCompensation = 0;
    private static int mOnCreateCount = 0;
    private MainHandler mMainHandler;
    private boolean mPaused;
    private boolean mKeepScreenOn;
    private boolean mHasCriticalPermissions;
    private boolean mIsForTest = false;
    private static final String FOR_TEST_FLAG = "isVoiceQuery";

    private PowerReceiver mPowerReceiver = null;
    private static final int LOW_BATTERY_LIMIT = 5;

    private static final int STATE_CREATE = 0;
    private static final int STATE_START = 1;
    private static final int STATE_RESUME = 2;
    private static final int STATE_PAUSED = 3;
    private static final int STATE_STOP = 4;
    private static final int STATE_DESTROY = 5;

    private int mState = STATE_CREATE;
    private boolean mHasError = false;
	private	PermissionLayoutManager  mPermissionLayoutManager=PermissionLayoutManager.getInstance();//add by heml,for kill awcamera
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AWCamera does not support split-screen
        if (isInMultiWindowMode()){
                Context context = null;
                try {
                    context = this.createPackageContext("com.android.systemui",
                            Context.CONTEXT_INCLUDE_CODE
                                    | Context.CONTEXT_IGNORE_SECURITY);
                    int stringId = context.getResources().getIdentifier(
                            "dock_non_resizeble_failed_to_dock_text", "string", context.getPackageName());
                    String toast = context.getResources().getString(stringId);
                    Toast.makeText(this, toast, Toast.LENGTH_SHORT).show();
                } catch (PackageManager.NameNotFoundException ex) {
                    Log.e(TAG, "[onCreate] NameNotFoundException ", ex);
                }
                finish();
            }
        mState = STATE_CREATE;
        Log.e(TAG,"onCreate mState="+mState);
        mOnCreateCount++;
        Log.e(TAG,"onCreate mOnCreateCount=" + mOnCreateCount);
       // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);
        mIsForTest = isVoiceInteractionRoot();
        if (getIntent().hasExtra(FOR_TEST_FLAG)) {
            mIsForTest = mIsForTest || getIntent().getBooleanExtra(FOR_TEST_FLAG, false);
        }
        Log.w(TAG, "mIsForTest: " + mIsForTest);
		mPermissionLayoutManager.setListener(this);//add by heml,for kill awcamera
        checkPermissions();
        if (!mHasCriticalPermissions) {
            Log.v(TAG, "onCreate: Missing critical permissions.");
            finish();
            return;
        }

        checkPowerStatus();

        CameraData.getInstance().setActivity(this);
        CameraData.getInstance().initData(getApplicationContext());
        String action = getIntent().getAction();
        CameraData.getInstance().setImageCaptureIntent(MediaStore.ACTION_IMAGE_CAPTURE.equals(action)||MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action));
        CameraData.getInstance().setSecureCameraIntent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE.equals(action)||MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE.equals(action)||getIntent().getBooleanExtra(SECURE_CAMERA_EXTRA, false));
        CameraData.getInstance().setVideoCaptureIntent((MediaStore.ACTION_VIDEO_CAPTURE.equals(action)));
        CameraManager.getInstance().startThread();
        View rootView = findViewById(R.id.root_camera);
        mUiManager = new UIManager(getApplicationContext(), this);
        mMainHandler = new MainHandler(this, getMainLooper());
        mUiManager.initView(rootView);
        Configuration config = getResources().getConfiguration();
        mViewPagerFragment = new PaperFragment();
        mViewPagerFragment.setUiManager(mUiManager);
        getFragmentManager().beginTransaction()
                .replace(R.id.paper_frament_container, mViewPagerFragment).commit();

        mOrientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                CameraData.getInstance().setOrientation(orientation);
                int orientationCompensation = (CameraData.getInstance().getOrientation() +CameraData.getInstance().getDisplayRotation() )% 360;

                if(mOrientationCompensation != orientationCompensation){
                    mOrientationCompensation = orientationCompensation;
                    mUiManager.onOrientationChanged(mOrientationCompensation);
                    CameraData.getInstance().setOrientationCompensation(mOrientationCompensation);
                }
            }
        };
    }

    private static class MainHandler extends Handler {
        final WeakReference<CameraActivity> mActivity;

        public MainHandler(CameraActivity activity, Looper looper) {
            super(looper);
            mActivity = new WeakReference<CameraActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            CameraActivity activity = mActivity.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {

                case MSG_CLEAR_SCREEN_ON_FLAG: {
                    if (!activity.mPaused) {
                        activity.getWindow().clearFlags(
                                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }
                    break;
                }
            }
        }
    }
    public void enableKeepScreenOn(boolean enabled) {
        if (mPaused) {
            return;
        }
        mKeepScreenOn = enabled;
        if (mKeepScreenOn) {
            mMainHandler.removeMessages(MSG_CLEAR_SCREEN_ON_FLAG);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            keepScreenOnForAWhile();
        }
    }
    private void keepScreenOnForAWhile() {
        if (mKeepScreenOn) {
            return;
        }
        mMainHandler.removeMessages(MSG_CLEAR_SCREEN_ON_FLAG);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mMainHandler.sendEmptyMessageDelayed(MSG_CLEAR_SCREEN_ON_FLAG, SCREEN_DELAY_MS);
    }

    private void resetScreenOn() {
        mKeepScreenOn = false;
        if(mMainHandler!=null) {
            mMainHandler.removeMessages(MSG_CLEAR_SCREEN_ON_FLAG);
        }
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    @Override
    public void onUserInteraction(){
        Log.e(TAG,"onUserInteraction");
        if (!isFinishing()) {
            keepScreenOnForAWhile();
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG,"onStart");
        mState = STATE_START;
        Log.e(TAG,"onStart mState="+mState);
        if(mUiManager!=null) {
            mUiManager.onStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG,"onPause");
        mState = STATE_PAUSED;
        Log.e(TAG,"onPause mState="+mState);
        mPaused = true;
        resetScreenOn();
        if(mOrientationEventListener!=null) {
            mOrientationEventListener.disable();
        }
        if(mUiManager!= null) {
            mUiManager.onPause();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        Log.e(TAG,"onResume");
        mState = STATE_RESUME;
        Log.e(TAG,"onResume mState="+mState);
        checkPermissions();
        if (!mHasCriticalPermissions) {
            Log.v(TAG, "onResume: Missing critical permissions.");
            finish();
            return;
        }
        mPaused = false;
        mOrientationEventListener.enable();
        keepScreenOnForAWhile();
        mUiManager.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG,"onStop");
        Log.e(TAG,"onStop mState="+mState);
        if (mState != STATE_PAUSED) {
            finish();
            return;
        }
        mState = STATE_STOP;
        if(mUiManager!= null) {
            mUiManager.onStop();
        }
    }
    //add by heml,for kill awcamera
    @Override
    public void finishCamera() {
		finish();
		String[] str = new String[]{"com.softwinner.camera","com.google.android.permissioncontroller"};
		sendBroadCastKill(str);

    }

    public void sendBroadCastKill(String[] pkgs){
        Intent intent = new Intent();
        intent.setAction("com.android.kill_awcamera");
        intent.addFlags(0x01000000);
        intent.putExtra("PACKAGE_NAME",pkgs);
        sendBroadcast(intent);
    }
    //add by heml,for kill awcamera
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mOnCreateCount--;
        Log.e(TAG,"onDestroy mOnCreateCount=" + mOnCreateCount);
        mState = STATE_DESTROY;
        if (mOnCreateCount == 0) {
            mPaused = false;
            new UiManagerReleaseThread(this).start();
            if(CameraManager.getInstance() != null) {
                CameraManager.getInstance().onDestroy();
            }
        }
    }
    public void release(){
        if(mUiManager!= null) {
            mUiManager.onDestroy();
        }
    }
    private static class UiManagerReleaseThread extends Thread {
        WeakReference<CameraActivity> mThreadActivityRef;

        public UiManagerReleaseThread(CameraActivity activity) {
            mThreadActivityRef = new WeakReference<CameraActivity>(
                    activity);
        }

        @Override
        public void run() {
            super.run();
            if (mThreadActivityRef == null)
                return;
            if (mThreadActivityRef.get() != null)
                mThreadActivityRef.get().release();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {
        if ((checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
            (boolean) SharedPreferencesUtils.getParam(this, Contants.KEY_HAS_SEEN_PERMISSIONS_DIALOGS, false))
            || mIsForTest) {
            mHasCriticalPermissions = true;
        } else {
            mHasCriticalPermissions = false;
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q && mHasCriticalPermissions) {
            mHasCriticalPermissions =
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        if (!mHasCriticalPermissions) {
            // TODO: Convert PermissionsActivity into a dialog so we
            // don't lose the state of CameraActivity.
            Intent intent = new Intent(this, PermissionActivity.class);
            startActivity(intent);
            //finish();//add by heml,for kill awcamera
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.i(TAG,"dispatchTouchEvent");
        if(!CameraData.getInstance().getIsPreviewDone()||CameraData.getInstance().getIsStorageError()){
            return false;
        }
        return super.dispatchTouchEvent(ev);
     //
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.i(TAG,"dispatchKeyEvent :"+CameraData.getInstance().getIsPreviewDone());
        if(CameraData.getInstance().getIsStorageError() && event.getKeyCode()!=KEYCODE_BACK   ){
            return false;
        }
        if(!CameraData.getInstance().getIsPreviewDone()){
            return false;
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        if(!CameraData.getInstance().getIsPreviewDone()||CameraData.getInstance().getIsStorageError()){
            return false;
        }
        return super.dispatchTrackballEvent(ev);
    }

    public void checkPowerStatus() {
        mPowerReceiver = new PowerReceiver();
        mPowerReceiver.setCallback(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mPowerReceiver, intentFilter);
    }

    @Override
    public void remainLevel(int level) {
        if (mPowerReceiver != null) unregisterReceiver(mPowerReceiver);
        if (level < LOW_BATTERY_LIMIT) {
            Toast.makeText(this, R.string.battery_remain_too_low, Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
