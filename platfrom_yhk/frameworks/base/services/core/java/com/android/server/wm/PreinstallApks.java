package com.android.server.wm;

import android.os.Bundle;
import android.content.pm.IPackageInstallObserver2;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageInstaller.SessionParams;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import java.util.ArrayList;

import android.os.RemoteException;
import android.net.Uri;
import android.os.IBinder;

import libcore.io.IoUtils;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.IPackageManager;
import android.content.pm.ApplicationInfo;
import android.app.AppGlobals;
//add by heml,for install apk before systemready
public class PreinstallApks {
    public static final String TAG = "PreinstallApks";

    public abstract class PreinstallCallback {
        public abstract void onDone(Bundle mBd);
    }

    private PreinstallCallback mCb;
    private static PreinstallApks mInstance = null;
    private static Object obj = new Object();
    private HandlerThread mInstallThread = new HandlerThread("install");
    private Handler mInstallHandler = null;
    private static final int MSG_INSTALL_START = 1;

    //private static final String PREINSTALL_PATH_SYSTEM = "/system/preinstall-inboot";
	private static final String PREINSTALL_PATH_SYSTEM = "/product/preinstall-inboot";
    private static final String PREINSTALL_PATH_VENDOR = "/vendor/preinstall-inboot";
    private int mPreinstallSum = 0;
    private boolean hasStarted = false;
    private boolean hasStartedInstall = false;
    private static Context mContext;

    private static final long MAX_WAIT_TIME = 60 * 1000;
    private static final long WAIT_TIME_INCR = 1400;

    private PreinstallApks(Context ctx) {
        mContext = ctx;
        mInstallThread.start();
        mInstallHandler = new Handler(mInstallThread.getLooper()) {
            public void handleMessage(Message msg) {
                if (msg.what == MSG_INSTALL_START) {
                    Bundle mBD = msg.getData();
                    int index = mBD.getInt("index");
                    int userid = mBD.getInt("userid");
                    int displayId = mBD.getInt("displayid");
                    String path = mBD.getString("path");
                    String reason = mBD.getString("reason");
                    InstallReceiver receiver = new InstallReceiver(getApkPackage(path));
                    invokeInstallPackage(Uri.parse(path), 0, receiver);
                    Log.d(TAG, "install done index=" + index + ", sum=" + mPreinstallSum);
                    if (index == mPreinstallSum - 1) {
                        if (mCb != null) {
                            hasStarted = true;
                            mCb.onDone(mBD);
                        }
                    }
                }
            }
        };
    }

    public static PreinstallApks getInstance(Context ctx) {
        synchronized (obj) {
            if (mInstance == null) {
                mInstance = new PreinstallApks(ctx);
            }
        }
        return mInstance;
    }

    public void setPreinstallCallback(PreinstallCallback cb) {
        mCb = cb;
    }

    private ArrayList<String> ergodicListFiles(File dir, ArrayList<String> filesList) {
        if (!dir.exists()) {
            Log.e(TAG, "ergodicListFiles: directory " + dir + " is not exist");
            return filesList;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return filesList;
        }
        for (File f : files) {
            if (f.isDirectory()) {
                ergodicListFiles(f, filesList);
            } else if (f.getName().toLowerCase().endsWith(".apk") || f.getName().toLowerCase().endsWith(".dj")) {
                filesList.add(f.getAbsolutePath());
            }
        }
        return filesList;
    }

    private abstract static class GenericReceiver extends BroadcastReceiver {
        private boolean doneFlag = false;
        boolean received = false;
        Intent intent;
        IntentFilter filter;

        abstract boolean notifyNow(Intent intent);

        @Override
        public void onReceive(Context context, Intent intent) {
            if (notifyNow(intent)) {
                synchronized (this) {
                    received = true;
                    doneFlag = true;
                    this.intent = intent;
                    notifyAll();
                }
            }
        }

        public boolean isDone() {
            return doneFlag;
        }

        public void setFilter(IntentFilter filter) {
            this.filter = filter;
        }
    }

    private static class InstallReceiver extends GenericReceiver {
        String pkgName;

        InstallReceiver(String pkgName) {
            this.pkgName = pkgName;
            IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
            filter.addDataScheme("package");
            super.setFilter(filter);
        }

        public boolean notifyNow(Intent intent) {
            String action = intent.getAction();
            if (!Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                return false;
            }
            Uri data = intent.getData();
            String installedPkg = data.getEncodedSchemeSpecificPart();
            if (pkgName.equals(installedPkg)) {
                return true;
            }
            return false;
        }
    }

    private static class LocalIntentReceiver {
        private final SynchronousQueue<Intent> mResult = new SynchronousQueue<>();
        private IIntentSender.Stub mLocalSender = new IIntentSender.Stub() {
            @Override
            public void send(int code, Intent intent, String resolvedType, IBinder whitelistToken,
                             IIntentReceiver finishedReceiver, String requiredPermission, Bundle options) {
                try {
                    mResult.offer(intent, 5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        public IntentSender getIntentSender() {
            return new IntentSender((IIntentSender) mLocalSender);
        }

        public Intent getResult() {
            try {
                return mResult.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void writeSplitToInstallSession(PackageInstaller.Session session, String inPath,
                                            String splitName) throws RemoteException {
        long sizeBytes = 0;
        final File file = new File(inPath);
        if (file.isFile()) {
            sizeBytes = file.length();
        } else {
            return;
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            in = new FileInputStream(inPath);
            out = session.openWrite(splitName, 0, sizeBytes);

            int total = 0;
            byte[] buffer = new byte[65536];
            int c;
            while ((c = in.read(buffer)) != -1) {
                total += c;
                out.write(buffer, 0, c);
            }
            session.fsync(out);
        } catch (IOException e) {
            Log.d(TAG, "IOException: failed to write; " + e.getMessage());
        } finally {
            IoUtils.closeQuietly(out);
            IoUtils.closeQuietly(in);
            IoUtils.closeQuietly(session);
        }
    }


    private void invokeInstallPackage(Uri packageUri, int flags, GenericReceiver receiver) {
        mContext.registerReceiver(receiver, receiver.filter);
        synchronized (receiver) {
            final String inPath = packageUri.getPath();
            PackageInstaller.Session session = null;
            Log.d(TAG, "invokeInstallPackage inPath=" + inPath);
            try {
                final SessionParams sessionParams = new SessionParams(SessionParams.MODE_FULL_INSTALL);
                sessionParams.installFlags |= PackageManager.INSTALL_ALL_WHITELIST_RESTRICTED_PERMISSIONS;
                sessionParams.installFlags |= PackageManager.INSTALL_GRANT_RUNTIME_PERMISSIONS;
                final int sessionId = mContext.getPackageManager().getPackageInstaller().createSession(sessionParams);
                session = mContext.getPackageManager().getPackageInstaller().openSession(sessionId);
                writeSplitToInstallSession(session, inPath, "base.apk");
                final LocalIntentReceiver localReceiver = new LocalIntentReceiver();
                session.commit(localReceiver.getIntentSender());
                final Intent result = localReceiver.getResult();
                final int status = result.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
                Log.d(TAG, "invokeInstallPackage status=" + status);
                if (status != PackageInstaller.STATUS_SUCCESS) {
                    return;
                }

                // Verify we received the broadcast
                long waitTime = 0;
                while ((!receiver.isDone()) && (waitTime < MAX_WAIT_TIME)) {
                    try {
                        receiver.wait(WAIT_TIME_INCR);
                        waitTime += WAIT_TIME_INCR;
                    } catch (InterruptedException e) {
                        Log.i(TAG, "Interrupted during sleep", e);
                    }
                }
                if (!receiver.isDone()) {
                    //fail("Timed out waiting for PACKAGE_ADDED notification");
                    Log.d(TAG, "Timed out waiting for PACKAGE_ADDED notification.");
                } else {
                    Log.d(TAG, "Install apk done, PACKAGE_ADDED notification.");
                }
            } catch (IllegalArgumentException | IOException | RemoteException e) {
                Log.w(TAG, "Failed to install package; path=" + inPath, e);
                //fail("Failed to install package; path=" + inPath + ", e=" + e);
            } finally {
                IoUtils.closeQuietly(session);
                mContext.unregisterReceiver(receiver);
            }
        }
    }

    private String getApkPackage(String apkPath) {
        PackageManager mPM = mContext.getPackageManager();
        PackageInfo mPkgInfo = mPM.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if (mPkgInfo != null) {
            ApplicationInfo mAppInfo = mPkgInfo.applicationInfo;
            Log.d(TAG, "apkPath=" + apkPath + ", pkg=" + mAppInfo.packageName + ", clsName=" + mAppInfo.className);
            return mAppInfo.packageName;
        } else {
            Log.d(TAG, "apkPath=" + apkPath + ", mPkgInfo is null.");
            return null;
        }
    }

    public boolean startPreinstall(Bundle mData) {
        boolean mIsFirstBoot = false;
        if (!hasStarted) {
            IPackageManager pm = AppGlobals.getPackageManager();
            try {
                mIsFirstBoot = pm.isFirstBoot();
            } catch (Exception e) {
            }
            Log.d(TAG, "startPreinstall mFirstBoot=" + mIsFirstBoot);
            if (mIsFirstBoot) {
                ArrayList<String> mApkPaths = new ArrayList<String>();
                ergodicListFiles(new File(PREINSTALL_PATH_SYSTEM), mApkPaths);
                ergodicListFiles(new File(PREINSTALL_PATH_VENDOR), mApkPaths);
                int apkSize = mApkPaths.size();
                mPreinstallSum = apkSize;
                Log.d(TAG, "preinstall apk sum=" + apkSize + ", hasStartedInstall=" + hasStartedInstall);
                if (hasStartedInstall) {
                    return true;
                }
                hasStartedInstall = true;
                if (apkSize == 0) {
                    if (mCb != null) {
                        hasStarted = true;
                        mCb.onDone(mData);
                    }
                    return true;
                }
                try {
                    for (int i = 0; i < apkSize; ++i) {
                        final int tmpIndex = i;
                        Log.d(TAG, "start install apk=" + mApkPaths.get(i) + ", i=" + i);
                        Bundle mBD = new Bundle(mData);
                        mBD.putInt("index", i);
                        mBD.putString("path", mApkPaths.get(i));
                        Message msg = mInstallHandler.obtainMessage(MSG_INSTALL_START);
                        msg.setData(mBD);
                        msg.sendToTarget();
                    }
                } catch (Exception e) {
                }
            } else {
                if (mCb != null) {
                    hasStarted = true;
                    mCb.onDone(mData);
                }
            }
        } else {
            if (mCb != null) {
                hasStarted = true;
                mCb.onDone(mData);
            }
        }
        return true;
    }

    public boolean canStartHome() {
        IPackageManager pm = AppGlobals.getPackageManager();
        boolean m_first_boot = false;
        try {
            m_first_boot = pm.isFirstBoot();
        } catch (Exception e) {
        }
        boolean result = m_first_boot ? hasStarted : true;
        Log.d(TAG, "canStartHome m_first_boot=" + m_first_boot + ", hasStarted=" + hasStarted + ", result=" + result);
        return result;
    }
}
