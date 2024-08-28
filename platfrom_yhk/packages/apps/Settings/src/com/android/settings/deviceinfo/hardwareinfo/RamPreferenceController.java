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
import android.os.SystemProperties;
import android.text.format.Formatter;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.slices.Sliceable;
import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RamPreferenceController extends BasePreferenceController {
    private static final String TAG = "RamPreferenceController";
    private static final String CONFIG_RAM_SIZE = "ro.deviceinfo.ram";
    private static final String SPRD_RAM_SIZE = "ro.boot.ddrsize";
    private final static String PROPERTY_DRAM_SIZE = "ro.boot.dramsize";

    private static final int SI_UNITS = 1000;
    private static final int IEC_UNITS = 1024;

    private Context mContext;
    public RamPreferenceController(Context context, String preferenceKey) {
        super(context, preferenceKey);
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
        /*
        String ramSize = getConfigRam();
        if (ramSize == null) {
            ramSize = getRamSizeFromProperty();
        }

        return ramSize;
        */
        float size = (float)SystemProperties.getInt(PROPERTY_DRAM_SIZE, 0)/1024.0f;
        return  String.format("%.1f", size)+ "GB";
    }
        public String getConfigRam() {
        String ramConfig = SystemProperties.get(CONFIG_RAM_SIZE, "unconfig");
        if ("unconfig".equals(ramConfig)) {
            Log.d(TAG, "no config ram size.");
            return null;
        } else {
            try {
                long configTotalRam = Long.parseLong(ramConfig);
                Log.d(TAG, "config ram to be: " + configTotalRam);
                return Formatter.formatShortFileSize(mContext, configTotalRam);
            } catch (NumberFormatException e) {
                Log.e(TAG, "config ram format error: " + e.getMessage());
                return null;
            }
        }
    }

    public String getRamSizeFromProperty() {
        String size = SystemProperties.get(SPRD_RAM_SIZE, "unconfig");
        if ("unconfig".equals(size)) {
            Log.d(TAG, "can not get ram size from "+SPRD_RAM_SIZE);
            return null;
        } else {
            Log.d(TAG, "property value is:" + size);
            String regEx="[^0-9]";
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(size);
            size = m.replaceAll("").trim();
            long ramSize = Long.parseLong(size);
            return Formatter.formatShortFileSize(mContext, covertUnitsToSI(ramSize));
        }
    }

    /**
     * SI_UNITS = 1000bytes; IEC_UNITS = 1024bytes
     * 512MB = 512 * 1000 * 1000
     * 2048MB = 2048/1024 * 1000 * 1000 * 1000
     * 2000MB = 2000 * 1000 * 1000
     */
    private long covertUnitsToSI(long size) {
        if (size > SI_UNITS && size % IEC_UNITS == 0) {
            return size / IEC_UNITS * SI_UNITS * SI_UNITS * SI_UNITS;
        }
        return size * SI_UNITS * SI_UNITS;
    }
}
