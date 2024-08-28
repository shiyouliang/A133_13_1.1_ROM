package com.softwinner.runin;

import com.softwinner.log.CrashApplication;
import com.softwinner.xml.Node;

import android.app.ActivityManager;
import android.content.SharedPreferences;
import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author zengsc
 * @version date 2013-6-6
 */
public class RuninApplication extends CrashApplication {
    public static final String TAG = "Runin-RuninApplication";
    private static final String SHARED_PACKAGE = "com.softwinner.runin";
    private SharedContext mContext;
    private Node mNode;
    private Node mNodeLocal;
    public String mAgingCfg;
    private SharedPreferences mPrfs;
    private SharedPreferences mPrfsYhk;
    PowerManager mPowerManager;
    public PowerManager.WakeLock mCpuWakeLock;
    public PowerManager.WakeLock mScreenWakeLock;
    public boolean mAgCfgFromSdcard;
    public boolean mBatteryPresent = false;
    private HandlerThread mHThread = new HandlerThread("clear_lost_dir");
    private Handler mClearHandler = null;
    private static final int MSG_CLEAR_LOST = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = new SharedContext(this, SHARED_PACKAGE);
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        SystemProperties.set("debug.test.runin.new", "1");
        mHThread.start();
        mClearHandler = new Handler(mHThread.getLooper()) {
            public void handleMessage(Message msg) {
                if (msg.what == MSG_CLEAR_LOST) {
                    String path = Utils.getAgingConfig(RuninApplication.this);
                    if (path == null || path.isEmpty()) {
                        Log.d(TAG, "MSG_CLEAR_LOST path=" + path);
                        return;
                    }
                    path = path.substring(0, path.lastIndexOf("/"));
                    File mFile = new File(path + "/LOST.DIR");
                    Log.d(TAG, "start check file=" + mFile.getPath());
                    if (!mFile.exists()) {
                        Log.d(TAG, "file=" + mFile.getPath() + " not exist!");
                    } else if (!mFile.canWrite()) {
                        Log.d(TAG, "file=" + mFile.getPath() + " can not write!");
                    } else {
                        File[] lostFiles = mFile.listFiles();
                        Log.d(TAG, "lostFiles size=" + (lostFiles == null ? 0 : lostFiles.length) + ", lostFiles=" + lostFiles);
                        for (int i = 0; lostFiles != null && i < lostFiles.length; ++i) {
                            File tmpFile = lostFiles[i];
                            boolean ret = tmpFile.delete();
                            Log.d(TAG, "i=" + i + ", name=" + tmpFile.getName() + " delete=" + ret);
                        }
                    }
                }
            }
        };
    }

    public void clearLostDir() {
        Log.d(TAG, "clearLostDir.");
        mClearHandler.sendEmptyMessageDelayed(MSG_CLEAR_LOST, 6000);
    }

    /**
     * get node
     */
    public Node getNode() {
        if (mNode == null) {
            mNode = Utils.getSettings(mAgingCfg);
                        /*
			if (mNode == null) {
				mNode = Utils.getDefaultSettings(mContext);
			}*/
        }
        return mNode;
    }

    public Node getNodeLocal() {
        if (mNodeLocal == null) {
            mNodeLocal = Utils.getSettings();
            if (mNodeLocal == null) {
                mNodeLocal = Utils.getDefaultSettings(mContext);
            }
        }

        mPrfsYhk = getSharedPreferences("yhk_data", MODE_PRIVATE);
        if (mPrfsYhk.getBoolean("isFisrtOpen", true) && mNodeLocal.getNode("Foreground") != null && mNodeLocal.getNode("Foreground").getNode("MemtesterTest") != null) {
            mNodeLocal.getNode("Foreground").getNode("MemtesterTest").getAttribute("memsize").setValue("" + GetDDRTestNum());
            Utils.saveSetting(mNodeLocal);
            mPrfsYhk.edit().putBoolean("isFisrtOpen", false).commit();
        }
        return mNodeLocal;
    }

    private long GetDDRTestNum() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        manager.getMemoryInfo(info);
        Log.e("yulh", "RuninApplication GetDDRTestNum info.totalMem =" + info.totalMem);
        long drrText = info.totalMem;
        double ss = (double) drrText / 1024 / 1024 / 1024;
        if (ss > 3) {
            return 2048;
        }
        return 1024;


    }

    /**
     * reload node
     */
    public Node reloadNode() {
        mNode = Utils.getSettings(mAgingCfg);
                /*
		if (mNode == null) {
			mNode = Utils.getDefaultSettings(mContext);
		}*/
        return mNode;
    }

    /**
     * reset node
     */
    public Node resetNode() {
        //mNode = Utils.getDefaultSettings(mContext);
        if (mAgingCfg != null) {
            mNode = Utils.getSettings(mAgingCfg);
        } else {
            mNode = Utils.getDefaultSettings(mContext);
        }
        mNodeLocal=mNode;
        if (mNodeLocal.getNode("Foreground") != null && mNodeLocal.getNode("Foreground").getNode("MemtesterTest") != null) {
            mNodeLocal.getNode("Foreground").getNode("MemtesterTest").getAttribute("memsize").setValue("" + GetDDRTestNum());
        }
        Utils.saveSetting(mNodeLocal);
        return mNode;
    }

    /**
     * save node
     */
    public void saveNode() {
        if (mNodeLocal == null) {
            mNodeLocal = getNodeLocal();
        }
        Utils.saveSetting(mNodeLocal);
    }

    public void setNeedClear(boolean need) {
        mPrfs = getApplicationContext().getSharedPreferences("clear_flag", Context.MODE_PRIVATE);
        mPrfs.edit().putBoolean("need_clear", need).commit();
    }

    public boolean getNeedClear() {
        mPrfs = getApplicationContext().getSharedPreferences("clear_flag", Context.MODE_PRIVATE);
        return mPrfs.getBoolean("need_clear", false);
    }

    public void cpuWakeLockAcquire() {
        if (mCpuWakeLock == null) {
            mCpuWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dragonrunin_cpu_lock");
        }
        if (mCpuWakeLock != null && !mCpuWakeLock.isHeld()) {
            mCpuWakeLock.acquire();
            Log.d(TAG, "cpuWakeLockAcquire: mCpuWakeLock.acquire()");
        }
    }

    public void cpuWakeLockRelease() {
        if (mCpuWakeLock != null && mCpuWakeLock.isHeld()) {
            mCpuWakeLock.release();
            Log.d(TAG, "cpuWakeLockRelease: mCpuWakeLock.release()");
        }
    }

    public void screenWakeLockAcquire() {
        if (mScreenWakeLock == null) {
            mScreenWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "dragonrunin_screen_lock");
        }
        if (mScreenWakeLock != null && !mScreenWakeLock.isHeld()) {
            mScreenWakeLock.acquire();
            Log.d(TAG, "screenWakeLockAcquire: mScreenWakeLock.acquire()");
        }
    }

    public void screenWakeLockRelease() {
        if (mScreenWakeLock != null && mScreenWakeLock.isHeld()) {
            mScreenWakeLock.release();
            Log.d(TAG, "screenWakeLockRelease: mScreenWakeLock.release()");
        }
    }

    public boolean isAgCfgFromSdcard() {
        return mAgCfgFromSdcard;
    }

    public void setAgCfgFromSdcard(boolean value) {
        mAgCfgFromSdcard = value;
    }

    public void unmountVolume(Context context, String destPath) {
        if (destPath == null) return;
        StorageManager mStorageManager = (StorageManager) context.getSystemService(StorageManager.class);
        List<VolumeInfo> volumes = mStorageManager.getVolumes();

        for (VolumeInfo vol : volumes) {
            if (VolumeInfo.TYPE_PUBLIC == vol.getType()) {
                String path = vol.getPath() != null ? vol.getPath().toString() : null;
                String internalPath = vol.getInternalPath() != null ? vol.getInternalPath().toString() : null;
                Log.d(TAG, "public volume: path = " + path.toString() + ", internalPath = " + internalPath + ", vol = " + vol);
                if (destPath != null &&
                        ((path != null && destPath.startsWith(path))
                                || (internalPath != null && destPath.startsWith(internalPath)))) {
                    Log.w(TAG, "unmount " + path);
                    new UnmountTask(context, vol).execute();
                }
            }
        }
    }

    public static class UnmountTask extends AsyncTask<Void, Void, Exception> {
        private final Context mContext;
        private final StorageManager mStorageManager;
        private final String mVolumeId;
        private final String mDescription;

        public UnmountTask(Context context, VolumeInfo volume) {
            mContext = context;
            mStorageManager = mContext.getSystemService(StorageManager.class);
            mVolumeId = volume.getId();
            mDescription = mStorageManager.getBestVolumeDescription(volume);
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                mStorageManager.unmount(mVolumeId);
                return null;
            } catch (Exception e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Exception e) {
            SystemProperties.set("debug.test.sd.unmounted", "1");
            if (e == null) {
                Log.d(TAG, "unmount " + mVolumeId + " success");
                Toast.makeText(mContext, "设备" + mDescription + "卸载成功！", Toast.LENGTH_LONG).show();
            } else {
                Log.e(TAG, "Failed to unmount " + mVolumeId, e);
                Toast.makeText(mContext, "设备" + mDescription + "卸载失败！", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void setBatteryPresent(boolean present) {
        mBatteryPresent = present;
        SystemProperties.set("debug.test.battery.present", present ? "1" : "0");
    }

    public boolean isBatteryPresent() {
        return mBatteryPresent;
    }
}
