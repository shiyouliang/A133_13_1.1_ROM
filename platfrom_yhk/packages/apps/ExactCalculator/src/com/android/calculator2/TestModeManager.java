package com.android.calculator2;

import java.io.File;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.SystemProperties;
import android.util.Log;
import android.widget.Toast;
import android.os.Bundle;
import com.android.calculator2.StorageHelper;
import java.util.List;

public class TestModeManager {
    private final static boolean debug = true;
    private final static String TAG = "TestModeManager";
    
    public final static String TEST_MODE_CONFIG = "23+";
    public final static String TEST_MODE_KEY = "33+";
    public final static String TEST_MODE_BATTERY = "43+";
    public final static String TEST_MODE_DEVICE = "63+";
    public final static String TEST_MODE_TOMAS = "65+";
	public final static String YHK_TEST_MODE_MONKEY = "652+";
	public final static String YHK_TEST_MODE_MONKEY_CONFIG = "651+";
    public final static String TEST_STRESS = "66+";
    public final static String TEST_PCB_TEST = "88+";
    public final static String TEST_APP_AWSYSTEMINFO = "888+";
    public final static String TEST_APP_KEEPTESTING = "999+";
	
    private final static String FLAG_INNER = "/system/etc/dragon_att_config.xml";
    private final static String FLAG_SDCARD = "/sdcard/DragonATT/dragon_att_config.xml";
    private final static String FLAG_AGING_SDCARD = "/sdcard/DragonATT/dragon_att_config_aging.xml";
    private final static String FLAG_SDCARD_CONFIG = "/sdcard/DragonATT/";
    private final static String FLAG_STORAGE = "/storage";
    private final static String FLAG_DRAGON_FILE = "DragonATT";
    private final static String FLAG_DRAGON_FILE_CONFIG = "DragonATT/dragon_att_config.xml";
    private final static String FLAG_DRAGON_FILE_AGING_CONFIG = "DragonATT/dragon_att_config_aging.xml";
    public final static String TEST_LOG_DEBUG_PERSISTED = "log(666+!)+";



    private final static int START_DRAGON_CONFIG = 110;
    private final static int START_DRAGON_TEST = 111;
    private final static int START_MODE_TOMAS = 112;
    private final static int START_BATTERY_TEST = 113;
    private final static int START_LOG_DEBUG = 114;
    private final static int START_STRESS_TEST = 115;
	private final static int START_DEVICES_TEST = 116;
    private final static int START_APP_KEEPTESTING = 117;
    private final static int START_PCB_TEST = 118;
    private final static int START_APP_AWSYSTEMINFO = 119;
	private final static int YHK_START_MONKEY_TEST = 998;
	private final static int YHK_START_MONKEY_TEST_CONFIG = 997;	

    StorageHelper mStorageHelper = null;
    private static Context mContext = null;

    public static boolean start(Context context, String inputKey) {
        Log.d(TAG, "inputkey=" + inputKey);
        // record start which one activity, if not, do not clear calculator content
        int status = 0;
        switch (inputKey) {
            case TEST_MODE_KEY:
                status = START_DRAGON_TEST;
                break;
            case TEST_MODE_CONFIG:
                status = START_DRAGON_CONFIG;
                break;
            case TEST_MODE_BATTERY:
                status = START_BATTERY_TEST;
                break;
			case TEST_MODE_DEVICE:
                status = START_DEVICES_TEST;
                break;
            case TEST_LOG_DEBUG_PERSISTED:
                status = START_LOG_DEBUG;
                break;
            case TEST_STRESS:
                status = START_STRESS_TEST;
                break;
            case TEST_PCB_TEST:
                status = START_PCB_TEST;
                break;
            case TEST_APP_KEEPTESTING:
                status = START_APP_KEEPTESTING;
                break;
            case TEST_APP_AWSYSTEMINFO:
                status = START_APP_AWSYSTEMINFO;
                break;
            case TEST_MODE_TOMAS:
			    status = START_MODE_TOMAS;
				break;
			case YHK_TEST_MODE_MONKEY:
                status = YHK_START_MONKEY_TEST;
                break;
			case YHK_TEST_MODE_MONKEY_CONFIG:
                status = YHK_START_MONKEY_TEST_CONFIG;
                break;	
       }
        if (status == 0) {
            return false;
        } else {
            mContext = context;
            return checkAndStart(status);
        }
    }

    private static boolean checkAndStart(int status) {
        boolean flag = false;
        if (debug) Log.d(TAG, "starting test");

        ComponentName component = null;
        Intent intent = new Intent();
        switch (status) {
            case START_DRAGON_CONFIG:
                Bundle bundle = new Bundle();
                bundle.putString("dragonatt_config", "true");
                intent.putExtras(bundle);
                // match any config file
                File file = findFile(FLAG_STORAGE, FLAG_DRAGON_FILE);
                if (!(file != null || new File(FLAG_SDCARD_CONFIG).exists() || new File(FLAG_INNER).exists())) {
                    return flag;
                }
            case START_DRAGON_TEST:
                if (status == START_DRAGON_TEST) {
                    // match any config file
                    File boardFile = findFile(FLAG_STORAGE, FLAG_DRAGON_FILE_CONFIG);
                    File agingFile = findFile(FLAG_STORAGE, FLAG_DRAGON_FILE_AGING_CONFIG);
                    if (!(boardFile != null || agingFile != null || new File(FLAG_SDCARD).exists() || new File(FLAG_AGING_SDCARD).exists() || new File(FLAG_INNER).exists())) {
                        return flag;
                    }
                }
                component = new ComponentName(
                        "com.softwinner.dragonatt",
                        "com.softwinner.dragonatt.ui.MainActivity");
                break;
            case START_BATTERY_TEST:
                component = new ComponentName(
                        "com.allwinner.batterytest",
                        "com.allwinner.batterytest.MainActivity");
                break;
			case START_DEVICES_TEST:
                component = new ComponentName(
                        "com.yhk.devicenewtest",
                    	"com.yhk.devicenewtest.MainActivity");
                break;

            case START_LOG_DEBUG:
                component = new ComponentName(
                        "com.softwinner.awlogsettings",
                        "com.softwinner.awlogsettings.MainActivity");
                break;
            case START_STRESS_TEST:
                component = new ComponentName(
                        "com.cghs.stresstest",
                        "com.cghs.stresstest.StressTestActivity");
                break;
            case START_PCB_TEST:
                component = new ComponentName(
                        "com.sktl.pcbtest",
                        "com.sktl.pcbtest.SktlTools");
                break;
            case START_APP_KEEPTESTING:
                component = new ComponentName(
                        "com.clock.pt1.keeptesting",
                        "com.clock.pt1.keeptesting.MainActivity");
                break;
            case START_MODE_TOMAS:
                component = new ComponentName(
                        "com.android.gl2jni",
                        "com.android.gl2jni.Main");
			    break;
            case START_APP_AWSYSTEMINFO:
                component = new ComponentName(
                        "com.softwinner.awsysteminfo",
                        "com.softwinner.awsysteminfo.MainActivity");
                Log.d(TAG, "START_APP_AWSYSTEMINFO");
                break;
			case YHK_START_MONKEY_TEST:
                component = new ComponentName(
                        "com.softwinner.runin",
                    	"com.softwinner.runin.activities.RuninActivity");
                break;
			case YHK_START_MONKEY_TEST_CONFIG:
                component = new ComponentName(
                        "com.softwinner.runin",
                    	"com.softwinner.runin.activities.ConfigActivity");
                break;
            default:
                break;
        }

        if (component == null) {
            return flag;
        }

        intent.setComponent(component);
        try {
            if (debug) Log.d(TAG, "start test");
            mContext.startActivity(intent);
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    private static File findFile(String dir, String path) {
        if (mContext == null) return null;
        StorageHelper storageHelper = new StorageHelper(mContext);
        List<String> mountPoints = storageHelper.getMountPoints();
        if (debug) Log.w(TAG, "mountPoints size: " + mountPoints.size());
        for (String item : mountPoints) {
            if (debug) Log.w(TAG, "file item: " + item);
            File file = new File(item);
            File[] list = file.listFiles();
            if (list == null) {
                continue;
            }
            if (debug) Log.w(TAG, "list size: " + list.length);
            File dfFile = new File(file, path);
            if (debug) Log.w(TAG, " dfFile path: " + dfFile.getAbsolutePath());
            if (dfFile.exists()) {
                return dfFile;
            }
        }
        return null;
    }
}

