/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.deviceinfo.hardwareinfo;

import android.content.Context;
import android.os.Build;

import com.android.settings.R;
import com.android.settings.applications.ProcStatsData;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.slices.Sliceable;
import android.os.SystemProperties;
import android.os.StatFs;
import android.text.format.Formatter;
import android.os.Environment;

import java.io.File;
import android.os.storage.StorageManager;

public class FlashPreferenceController extends BasePreferenceController {
    private Context mContext;
    private StorageManager mStorageManager;
    private static long sTotalInternalStorage;
    private final static String PROPERTY_STORAGE_SIZE = "ro.config.storage";

    public FlashPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
        mStorageManager = context.getSystemService(StorageManager.class);
        mContext=context;

    }

    @Override
    public int getAvailabilityStatus() {
        return UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
        //return getFlashTotal();
        float size = (float)SystemProperties.getInt(PROPERTY_STORAGE_SIZE, 0);
        return  String.format("%.1f", size)+ "GB";
    }
    /*
    private String getFlashTotal() {
        return getDataTotalSize(mContext);
    }

    public String getDataTotalSize(Context context){
    sTotalInternalStorage = mStorageManager.getPrimaryStorageSize();
    final String total = Formatter.formatFileSize(context, sTotalInternalStorage);
    return total;
    } */
}
