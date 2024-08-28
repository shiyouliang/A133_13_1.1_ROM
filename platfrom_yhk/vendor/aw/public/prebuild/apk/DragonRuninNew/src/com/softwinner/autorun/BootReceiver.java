package com.softwinner.autorun;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.util.Log;

import java.io.File;

import com.softwinner.runin.RuninApplication;
import com.softwinner.runin.Settings;
import com.softwinner.runin.Utils;

import android.os.PowerManager;
import android.os.Process;

/**
 * 开机启动广播监听，需将APK放入System/app下才能正常运行
 *
 * @author zengsc
 * @version date 2013-5-7
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "Runin-BootReceiver";
    private static boolean hasStarted = false;
    private Context mCtx = null;
    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                boolean present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
                ((RuninApplication) context.getApplicationContext()).setBatteryPresent(present);
                Log.d(TAG, "ACTION_BATTERY_CHANGED, present:" + present + ", intent = " + intent);
            }
        }
    };

    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        RuninApplication app = (RuninApplication) context.getApplicationContext();

        // add by yulh
        SharedPreferences sp = context.getSharedPreferences(Settings.SHARED_NAME, Context.MODE_PRIVATE);
        boolean is_aleady_open_hand = sp.getBoolean("is_aleady_open_hand", false);
        // add by yulh

        Log.d(TAG, "onReceive action=" + intent.getAction());
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            boolean restart = intent.getBooleanExtra("restart", false);
            String agingCfg = Utils.getAgingConfig(context);
            Log.d(TAG, "agingCfg=" + agingCfg);
//            if (agingCfg != null) {
//                app.mAgingCfg = agingCfg;
//                startRunin(context, restart);
//            } else {
//                if (app.getNeedClear()) {
//                    clearRunin(context);
//                }
//                app.cpuWakeLockRelease();
//            }
            if(is_aleady_open_hand || agingCfg != null){
                startRunin(context, restart);
            }else{
                if (app.getNeedClear()) {
                    clearRunin(context);
                }
                app.cpuWakeLockRelease();
            }
        } else if (intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
            app.cpuWakeLockAcquire();
            app.clearLostDir();
        }
    }

    /**
     * 开始自动化压力测试
     */
    public void startRunin(Context context, boolean restart) {
        Log.i(TAG, "start runin " + restart);
        Intent batteryIntent = context.getApplicationContext().registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        Log.d(TAG, "registerReceiver ACTION_BATTERY_CHANGED, intent = " + batteryIntent);
        if (batteryIntent != null) {
            boolean present = batteryIntent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false);
            ((RuninApplication) context.getApplicationContext()).setBatteryPresent(present);
            Log.d(TAG, "registerReceiver ACTION_BATTERY_CHANGED success, present:" + present + ", intent = " + batteryIntent);
        }

        Intent i = new Intent();
        ComponentName component = new ComponentName("com.softwinner.runin",
                "com.softwinner.runin.activities.RuninActivity");
        i.setComponent(component);
        if (restart) {
            i.putExtra("restart", restart);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    public void clearRunin(Context context) {
        Log.i(TAG, "clear runin");
        Intent i = new Intent();
        ComponentName component = new ComponentName("com.softwinner.runin",
                "com.softwinner.runin.activities.RuninActivity");
        i.setComponent(component);
        i.putExtra("clear_result", true);
        i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
