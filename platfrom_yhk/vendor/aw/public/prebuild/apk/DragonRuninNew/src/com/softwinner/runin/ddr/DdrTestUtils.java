package com.softwinner.runin.ddr;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

/* loaded from: classes.dex */
public class DdrTestUtils {
    public static int getTestCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    public static long getPreferMemSize(Context context, long j, int i) {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ((ActivityManager) context.getSystemService("activity")).getMemoryInfo(memoryInfo);
        android.util.Log.e("yulh","DdrTestUtils getPreferMemSize="+memoryInfo.availMem);
        long j2 = (memoryInfo.availMem * 2) / 3;
        Log.d("DdrTestUtils", "getPreferMemSize: maxTestableSize= " + (j2 >> 20) + "MB");
        if (j <= 0 || j > j2) {
            j = j2;
        }
        return j / i;
    }
}